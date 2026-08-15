package com.rolecall.application.controller;

import com.rolecall.application.dto.ApplicationResponse;
import com.rolecall.application.entity.ApplicationStatus;
import com.rolecall.application.security.SecurityConfig;
import com.rolecall.application.service.ApplicationService;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void applyingWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void applyingAsEmployerIsForbidden() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.claim("role", "EMPLOYER"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void applyingAsCandidateSucceeds() throws Exception {
        UUID candidateId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(applicationService.apply(any(), any())).thenReturn(new ApplicationResponse(
                UUID.randomUUID(), jobId, candidateId, ApplicationStatus.APPLIED, null, Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/applications")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject(candidateId.toString()).claim("role", "CANDIDATE"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\": \"" + jobId + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void gettingMyApplicationsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/applications/mine"))
                .andExpect(status().isUnauthorized());
    }
}
