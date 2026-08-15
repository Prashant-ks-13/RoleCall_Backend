package com.rolecall.auth.service;

import com.rolecall.auth.dto.AuthResponse;
import com.rolecall.auth.dto.LoginRequest;
import com.rolecall.auth.dto.RegisterRequest;
import com.rolecall.auth.entity.User;
import com.rolecall.auth.entity.UserRole;
import com.rolecall.auth.event.UserEventProducer;
import com.rolecall.auth.exception.EmailAlreadyExistsException;
import com.rolecall.auth.exception.InvalidCredentialsException;
import com.rolecall.auth.repository.UserRepository;
import com.rolecall.auth.security.jwt.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserEventProducer userEventProducer;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerDefaultsToCandidateRoleWhenNoneProvided() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", null);
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            return u;
        });
        UUID userId = UUID.randomUUID();
        when(refreshTokenService.issueFor(any()))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("raw-refresh", UUID.randomUUID(), userId));
        when(jwtTokenService.issueAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtTokenService.accessTokenTtlSeconds()).thenReturn(900L);

        AuthResponse response = authService.register(request);

        assertThat(response.role()).isEqualTo(UserRole.CANDIDATE.name());
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("dup@example.com", "password123", null);
        when(userRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void loginRejectsUnknownEmailWithGenericMessage() {
        LoginRequest request = new LoginRequest("nobody@example.com", "password123");
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginRejectsWrongPasswordWithSameGenericMessageAsUnknownEmail() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("real@example.com")
                .passwordHash("hashed")
                .role(UserRole.CANDIDATE)
                .enabled(true)
                .build();
        LoginRequest request = new LoginRequest("real@example.com", "wrong-password");
        when(userRepository.findByEmailIgnoreCase("real@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginSucceedsAndIssuesTokensForValidCredentials() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("real@example.com")
                .passwordHash("hashed")
                .role(UserRole.EMPLOYER)
                .enabled(true)
                .build();
        LoginRequest request = new LoginRequest("real@example.com", "correct-password");
        when(userRepository.findByEmailIgnoreCase("real@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);
        when(refreshTokenService.issueFor(user.getId()))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("raw-refresh", UUID.randomUUID(), user.getId()));
        when(jwtTokenService.issueAccessToken(user)).thenReturn("access-token");
        when(jwtTokenService.accessTokenTtlSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(request);

        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.role()).isEqualTo("EMPLOYER");
    }
}
