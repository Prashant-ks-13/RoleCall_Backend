package com.rolecall.payment.controller;

import com.rolecall.payment.dto.CheckoutSessionResponse;
import com.rolecall.payment.security.SecurityConfig;
import com.rolecall.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void checkoutSessionRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/payments/checkout-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkoutSessionRejectsCandidateRole() throws Exception {
        mockMvc.perform(post("/api/payments/checkout-session")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.claim("role", "CANDIDATE"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkoutSessionSucceedsForEmployer() throws Exception {
        when(paymentService.createCheckoutSession(any(), any()))
                .thenReturn(new CheckoutSessionResponse(UUID.randomUUID(), "https://checkout.stripe.com/cs_test"));

        mockMvc.perform(post("/api/payments/checkout-session")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject(UUID.randomUUID().toString()).claim("role", "EMPLOYER"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void webhookIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .header("Stripe-Signature", "t=123,v1=abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": \"evt_1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void gettingMyPaymentsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/payments/mine"))
                .andExpect(status().isUnauthorized());
    }
}
