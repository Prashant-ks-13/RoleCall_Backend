package com.rolecall.payment.controller;

import com.rolecall.payment.dto.CheckoutSessionRequest;
import com.rolecall.payment.dto.CheckoutSessionResponse;
import com.rolecall.payment.dto.PageResponse;
import com.rolecall.payment.dto.PaymentTransactionResponse;
import com.rolecall.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Razorpay (test-mode) payments for featured job listings")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout-session")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Create a Razorpay Payment Link to feature a job posting")
    public CheckoutSessionResponse createCheckoutSession(@AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody CheckoutSessionRequest request) {
        return paymentService.createCheckoutSession(userId(jwt), request);
    }

    @PostMapping("/webhook")
    @Operation(summary = "Razorpay webhook receiver (verified via X-Razorpay-Signature, not a bearer token)")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                         @RequestHeader("X-Razorpay-Signature") String signature,
                                         @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
        paymentService.handleWebhook(payload, signature, eventId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mine")
    @Operation(summary = "List the authenticated user's own payment transactions")
    public PageResponse<PaymentTransactionResponse> getMine(@AuthenticationPrincipal Jwt jwt,
                                                              @PageableDefault(size = 20) Pageable pageable) {
        return paymentService.getMine(userId(jwt), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single payment transaction (owner or admin only)")
    public PaymentTransactionResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return paymentService.getById(id, userId(jwt), isAdmin(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private boolean isAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaimAsString("role"));
    }
}
