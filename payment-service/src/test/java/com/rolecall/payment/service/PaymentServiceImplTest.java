package com.rolecall.payment.service;

import com.rolecall.payment.client.JobServiceClient;
import com.rolecall.payment.client.JobSummary;
import com.rolecall.payment.dto.CheckoutSessionRequest;
import com.rolecall.payment.dto.CheckoutSessionResponse;
import com.rolecall.payment.entity.PaymentStatus;
import com.rolecall.payment.entity.PaymentTransaction;
import com.rolecall.payment.entity.PaymentType;
import com.rolecall.payment.event.PaymentEventProducer;
import com.rolecall.payment.exception.InvalidWebhookSignatureException;
import com.rolecall.payment.exception.NotAuthorizedForPaymentException;
import com.rolecall.payment.exception.PaymentTransactionNotFoundException;
import com.rolecall.payment.repository.PaymentTransactionRepository;
import com.rolecall.payment.repository.WebhookEventRepository;
import com.rolecall.payment.stripe.StripeCheckoutSession;
import com.rolecall.payment.stripe.StripeGateway;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;
    @Mock
    private WebhookEventRepository webhookEventRepository;
    @Mock
    private JobServiceClient jobServiceClient;
    @Mock
    private StripeGateway stripeGateway;
    @Mock
    private PaymentEventProducer eventProducer;

    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(
                transactionRepository, webhookEventRepository, jobServiceClient, stripeGateway, eventProducer,
                4900L, "usd",
                "http://localhost:5173/success", "http://localhost:5173/cancel", "whsec_test");
    }

    @Test
    void checkoutSessionRejectsNonOwnerEmployer() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(jobServiceClient.getJob(jobId)).thenReturn(new JobSummary(jobId, UUID.randomUUID(), "ACTIVE"));

        assertThatThrownBy(() -> service.createCheckoutSession(requesterId, new CheckoutSessionRequest(jobId)))
                .isInstanceOf(NotAuthorizedForPaymentException.class);
        verify(stripeGateway, never()).createCheckoutSession(anyLong(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void checkoutSessionCreatesAPendingTransactionAndReturnsTheHostedUrl() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID employerId = UUID.randomUUID();
        when(jobServiceClient.getJob(jobId)).thenReturn(new JobSummary(jobId, employerId, "ACTIVE"));
        // Mimics JPA's @PrePersist assigning the id on first save — a mock save() doesn't
        // run entity lifecycle callbacks the way a real EntityManager.persist() would.
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> {
            PaymentTransaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            return t;
        });
        when(stripeGateway.createCheckoutSession(anyLong(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(new StripeCheckoutSession("cs_test_123", "https://checkout.stripe.com/cs_test_123"));

        CheckoutSessionResponse response = service.createCheckoutSession(employerId, new CheckoutSessionRequest(jobId));

        assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.com/cs_test_123");

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        PaymentTransaction saved = captor.getValue();
        assertThat(saved.getStripeSessionId()).isEqualTo("cs_test_123");
        assertThat(saved.getType()).isEqualTo(PaymentType.JOB_FEATURE);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void webhookRejectsInvalidSignature() throws Exception {
        when(stripeGateway.verifyWebhookSignature(anyString(), anyString(), anyString()))
                .thenThrow(new SignatureVerificationException("bad signature", "sig"));

        assertThatThrownBy(() -> service.handleWebhook("{}", "bad-sig"))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    @Test
    void webhookIsIdempotentForAlreadyProcessedEvents() throws Exception {
        Event event = new Event();
        event.setId("evt_123");
        event.setType("checkout.session.completed");
        when(stripeGateway.verifyWebhookSignature(anyString(), anyString(), anyString())).thenReturn(event);
        when(webhookEventRepository.existsByStripeEventId("evt_123")).thenReturn(true);

        service.handleWebhook("{}", "sig");

        verify(webhookEventRepository, never()).save(any());
        verify(transactionRepository, never()).findByStripeSessionId(any());
    }

    @Test
    void getByIdRejectsNonOwnerNonAdmin() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        PaymentTransaction transaction = PaymentTransaction.builder().id(id).userId(ownerId).build();
        when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> service.getById(id, otherUser, false))
                .isInstanceOf(NotAuthorizedForPaymentException.class);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id, UUID.randomUUID(), false))
                .isInstanceOf(PaymentTransactionNotFoundException.class);
    }
}
