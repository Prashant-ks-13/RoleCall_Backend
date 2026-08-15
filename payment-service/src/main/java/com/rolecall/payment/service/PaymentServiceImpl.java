package com.rolecall.payment.service;

import com.razorpay.RazorpayException;
import com.rolecall.payment.client.JobServiceClient;
import com.rolecall.payment.client.JobSummary;
import com.rolecall.payment.dto.CheckoutSessionRequest;
import com.rolecall.payment.dto.CheckoutSessionResponse;
import com.rolecall.payment.dto.PageResponse;
import com.rolecall.payment.dto.PaymentTransactionResponse;
import com.rolecall.payment.entity.PaymentStatus;
import com.rolecall.payment.entity.PaymentTransaction;
import com.rolecall.payment.entity.PaymentType;
import com.rolecall.payment.entity.WebhookEvent;
import com.rolecall.payment.event.PaymentEventProducer;
import com.rolecall.payment.exception.InvalidWebhookSignatureException;
import com.rolecall.payment.exception.NotAuthorizedForPaymentException;
import com.rolecall.payment.exception.PaymentGatewayException;
import com.rolecall.payment.exception.PaymentTransactionNotFoundException;
import com.rolecall.payment.razorpay.RazorpayGateway;
import com.rolecall.payment.razorpay.RazorpayPaymentLink;
import com.rolecall.payment.repository.PaymentTransactionRepository;
import com.rolecall.payment.repository.WebhookEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final JobServiceClient jobServiceClient;
    private final RazorpayGateway razorpayGateway;
    private final PaymentEventProducer eventProducer;

    private final long jobFeatureAmountPaise;
    private final String jobFeatureCurrency;
    private final String callbackUrl;
    private final String webhookSecret;

    // Money/URL/secret values are constructor-injected (not field @Value) so this
    // service behaves correctly whether Spring wires it or a test constructs it
    // directly — see the auth-service TTL bug fixed in feature/application-service
    // for what goes wrong when a @Value field is left to its Java default instead.
    public PaymentServiceImpl(PaymentTransactionRepository transactionRepository,
                               WebhookEventRepository webhookEventRepository,
                               JobServiceClient jobServiceClient,
                               RazorpayGateway razorpayGateway,
                               PaymentEventProducer eventProducer,
                               @Value("${payment.job-feature.amount-paise:490000}") long jobFeatureAmountPaise,
                               @Value("${payment.job-feature.currency:INR}") String jobFeatureCurrency,
                               @Value("${razorpay.callback-url}") String callbackUrl,
                               @Value("${razorpay.webhook-secret}") String webhookSecret) {
        this.transactionRepository = transactionRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.jobServiceClient = jobServiceClient;
        this.razorpayGateway = razorpayGateway;
        this.eventProducer = eventProducer;
        this.jobFeatureAmountPaise = jobFeatureAmountPaise;
        this.jobFeatureCurrency = jobFeatureCurrency;
        this.callbackUrl = callbackUrl;
        this.webhookSecret = webhookSecret;
    }

    @Override
    @Transactional
    public CheckoutSessionResponse createCheckoutSession(UUID userId, CheckoutSessionRequest request) {
        JobSummary job = jobServiceClient.getJob(request.jobId());
        if (!job.employerId().equals(userId)) {
            throw new NotAuthorizedForPaymentException("You do not own this job posting");
        }

        BigDecimal amount = BigDecimal.valueOf(jobFeatureAmountPaise, 2);
        PaymentTransaction transaction = PaymentTransaction.builder()
                .userId(userId)
                .jobId(request.jobId())
                .type(PaymentType.JOB_FEATURE)
                .amount(amount)
                .currency(jobFeatureCurrency)
                .status(PaymentStatus.PENDING)
                .build();
        transaction = transactionRepository.save(transaction);

        try {
            RazorpayPaymentLink paymentLink = razorpayGateway.createPaymentLink(
                    jobFeatureAmountPaise, jobFeatureCurrency, "Featured Job Listing",
                    callbackUrl,
                    Map.of("transactionId", transaction.getId().toString(),
                            "jobId", request.jobId().toString(),
                            "userId", userId.toString()));

            transaction.setRazorpayPaymentLinkId(paymentLink.id());
            transactionRepository.save(transaction);
            return new CheckoutSessionResponse(transaction.getId(), paymentLink.shortUrl());
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay payment link for job {}", request.jobId(), e);
            throw new PaymentGatewayException("Failed to create Razorpay payment link", e);
        }
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signatureHeader, String eventIdHeader) {
        try {
            razorpayGateway.verifyWebhookSignature(payload, signatureHeader, webhookSecret);
        } catch (RazorpayException e) {
            throw new InvalidWebhookSignatureException("Invalid Razorpay webhook signature");
        }

        JSONObject body;
        try {
            body = new JSONObject(payload);
        } catch (JSONException e) {
            throw new PaymentGatewayException("Could not parse Razorpay webhook payload", e);
        }

        String eventId = resolveEventId(eventIdHeader, payload);
        if (webhookEventRepository.existsByRazorpayEventId(eventId)) {
            log.info("Ignoring already-processed Razorpay event {}", eventId);
            return;
        }
        String eventType = body.optString("event", "unknown");
        webhookEventRepository.save(WebhookEvent.builder()
                .razorpayEventId(eventId)
                .type(eventType)
                .build());

        try {
            switch (eventType) {
                case "payment_link.paid" -> onPaymentLinkPaid(body);
                case "payment_link.cancelled", "payment_link.expired" -> onPaymentLinkFailed(body, eventType);
                default -> log.debug("Ignoring unhandled Razorpay event type {}", eventType);
            }
        } catch (JSONException e) {
            log.error("Malformed Razorpay webhook payload for event {} ({}): {}", eventId, eventType, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentTransactionResponse> getMine(UUID userId, Pageable pageable) {
        Page<PaymentTransaction> page = transactionRepository.findByUserId(userId, pageable);
        return PageResponse.from(page.map(PaymentTransactionResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransactionResponse getById(UUID id, UUID requesterId, boolean requesterIsAdmin) {
        PaymentTransaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new PaymentTransactionNotFoundException(id));
        if (!requesterIsAdmin && !transaction.getUserId().equals(requesterId)) {
            throw new NotAuthorizedForPaymentException("You do not own this payment transaction");
        }
        return PaymentTransactionResponse.from(transaction);
    }

    private void onPaymentLinkPaid(JSONObject body) {
        JSONObject payload = body.getJSONObject("payload");
        String paymentLinkId = payload.getJSONObject("payment_link").getJSONObject("entity").getString("id");
        String paymentId = payload.has("payment")
                ? payload.getJSONObject("payment").getJSONObject("entity").optString("id", null)
                : null;

        transactionRepository.findByRazorpayPaymentLinkId(paymentLinkId).ifPresentOrElse(transaction -> {
            transaction.setStatus(PaymentStatus.SUCCEEDED);
            transaction.setRazorpayPaymentId(paymentId);
            transactionRepository.save(transaction);
            eventProducer.publishCompleted(transaction);
        }, () -> log.warn("Received payment_link.paid for unknown payment link {}", paymentLinkId));
    }

    private void onPaymentLinkFailed(JSONObject body, String eventType) {
        JSONObject payload = body.getJSONObject("payload");
        String paymentLinkId = payload.getJSONObject("payment_link").getJSONObject("entity").getString("id");

        transactionRepository.findByRazorpayPaymentLinkId(paymentLinkId).ifPresentOrElse(transaction -> {
            transaction.setStatus(PaymentStatus.FAILED);
            transactionRepository.save(transaction);
            eventProducer.publishFailed(transaction, "Razorpay event: " + eventType);
        }, () -> log.warn("Received {} for unknown payment link {}", eventType, paymentLinkId));
    }

    /**
     * Razorpay sends an X-Razorpay-Event-Id header on webhook deliveries; that's the
     * primary idempotency key. If it's ever absent, fall back to hashing the raw
     * payload — this can't distinguish two distinct events that happen to have a
     * byte-identical body, but it does still deduplicate genuine redelivery retries.
     */
    private String resolveEventId(String eventIdHeader, String payload) {
        if (eventIdHeader != null && !eventIdHeader.isBlank()) {
            return eventIdHeader;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
