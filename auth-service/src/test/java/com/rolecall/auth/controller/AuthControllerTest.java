package com.rolecall.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolecall.auth.dto.AuthResponse;
import com.rolecall.auth.exception.EmailAlreadyExistsException;
import com.rolecall.auth.exception.InvalidCredentialsException;
import com.rolecall.auth.security.SecurityConfig;
import com.rolecall.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void registerRejectsBlankEmailWithValidationError() throws Exception {
        String body = """
                {"email": "", "password": "password123"}
                """;

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        String body = """
                {"email": "user@example.com", "password": "short"}
                """;

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturnsCreatedWithTokenPairOnSuccess() throws Exception {
        AuthResponse response = new AuthResponse("access", "refresh", "Bearer", 900L,
                UUID.randomUUID(), "user@example.com", "CANDIDATE");
        when(authService.register(any())).thenReturn(response);

        String body = """
                {"email": "user@example.com", "password": "password123"}
                """;

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.role").value("CANDIDATE"));
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("user@example.com"));

        String body = """
                {"email": "user@example.com", "password": "password123"}
                """;

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void loginReturnsUnauthorizedOnInvalidCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        String body = """
                {"email": "user@example.com", "password": "wrong-password"}
                """;

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
