package com.rolecall.payment.service;

import com.rolecall.payment.client.JobServiceClient;
import com.rolecall.payment.client.JobSummary;
import com.rolecall.payment.dto.CheckoutSessionRequest;
import com.rolecall.payment.dto.CheckoutSessionResponse;
import com.rolecall.payment.dto.PageResponse;
import com.rolecall.payment.dto.PaymentTransactionResponse;
import com.rolecall.payment.entity.PaymentStatus;
import com.rolecall.payment.entity.PaymentTransaction;
import com.rolecall.payment.entity.PaymentType;
import com.rolecall.payment.event.PaymentEventProducer;
import com.rolecall.payment.exception.NotAuthorizedForPaymentException;
import com.rolecall.payment.exception.PaymentGatewayException;
import com.rolecall.payment.exception.PaymentTransactionNotFoundException;
import com.rolecall.payment.repository.PaymentTransactionRepository;
import com.rolecall.payment.repository.WebhookEventRepository;
import com.rolecall.payment.stripe.StripeCheckoutSession;
import com.rolecall.payment.stripe.StripeGateway;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final JobServiceClient jobServiceClient;
    private final StripeGateway stripeGateway;
    private final PaymentEventProducer eventProducer;

    private final long jobFeatureAmountCents;
    private final String jobFeatureCurrency;
    private final String successUrl;
    private final String cancelUrl;
    private final String webhookSecret;

    // Money/URL/secret values are constructor-injected (not field @Value) so this
    // service behaves correctly whether Spring wires it or a test constructs it
    // directly — see the auth-service TTL bug fixed in feature/application-service
    // for what goes wrong when a @Value field is left to its Java default instead.
    public PaymentServiceImpl(PaymentTransactionRepository transactionRepository,
                               WebhookEventRepository webhookEventRepository,
                               JobServiceClient jobServiceClient,
                               StripeGateway stripeGateway,
                               PaymentEventProducer eventProducer,
                               @Value("${payment.job-feature.amount-cents:4900}") long jobFeatureAmountCents,
                               @Value("${payment.job-feature.currency:usd}") String jobFeatureCurrency,
                               @Value("${stripe.success-url}") String successUrl,
                               @Value("${stripe.cancel-url}") String cancelUrl,
                               @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.transactionRepository = transactionRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.jobServiceClient = jobServiceClient;
        this.stripeGateway = stripeGateway;
        this.eventProducer = eventProducer;
        this.jobFeatureAmountCents = jobFeatureAmountCents;
        this.jobFeatureCurrency = jobFeatureCurrency;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        this.webhookSecret = webhookSecret;
    }

    @Override
    @Transactional
    public CheckoutSessionResponse createCheckoutSession(UUID userId, CheckoutSessionRequest request) {
        JobSummary job = jobServiceClient.getJob(request.jobId());
        if (!job.employerId().equals(userId)) {
            throw new NotAuthorizedForPaymentException("You do not own this job posting");
        }

        BigDecimal amount = BigDecimal.valueOf(jobFeatureAmountCents, 2);
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
            StripeCheckoutSession session = stripeGateway.createCheckoutSession(
                    jobFeatureAmountCents, jobFeatureCurrency, "Featured Job Listing",
                    successUrl, cancelUrl,
                    Map.of("transactionId", transaction.getId().toString(),
                            "jobId", request.jobId().toString(),
                            "userId", userId.toString()));

            transaction.setStripeSessionId(session.sessionId());
            transactionRepository.save(transaction);
            return new CheckoutSessionResponse(transaction.getId(), session.checkoutUrl());
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to create Stripe checkout session", e);
        }
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        Event event;
        try {
            event = stripeGateway.verifyWebhookSignature(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new com.rolecall.payment.exception.InvalidWebhookSignatureException("Invalid Stripe webhook signature");
        }

        if (webhookEventRepository.existsByStripeEventId(event.getId())) {
            log.info("Ignoring already-processed Stripe event {}", event.getId());
            return;
        }
        webhookEventRepository.save(com.rolecall.payment.entity.WebhookEvent.builder()
                .stripeEventId(event.getId())
                .type(event.getType())
                .build());

        switch (event.getType()) {
            case "checkout.session.completed" -> onCheckoutCompleted(event);
            case "checkout.session.expired" -> onCheckoutExpired(event);
            default -> log.debug("Ignoring unhandled Stripe event type {}", event.getType());
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

    private void onCheckoutCompleted(Event event) {
        Session session = extractSession(event);
        transactionRepository.findByStripeSessionId(session.getId()).ifPresentOrElse(transaction -> {
            transaction.setStatus(PaymentStatus.SUCCEEDED);
            transaction.setStripePaymentIntentId(session.getPaymentIntent());
            transactionRepository.save(transaction);
            eventProducer.publishCompleted(transaction);
        }, () -> log.warn("Received checkout.session.completed for unknown session {}", session.getId()));
    }

    private void onCheckoutExpired(Event event) {
        Session session = extractSession(event);
        transactionRepository.findByStripeSessionId(session.getId()).ifPresentOrElse(transaction -> {
            transaction.setStatus(PaymentStatus.FAILED);
            transactionRepository.save(transaction);
            eventProducer.publishFailed(transaction, "Checkout session expired");
        }, () -> log.warn("Received checkout.session.expired for unknown session {}", session.getId()));
    }

    private Session extractSession(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject()
                .orElseThrow(() -> new PaymentGatewayException("Could not deserialize Stripe event payload", null));
        return (Session) stripeObject;
    }
}
