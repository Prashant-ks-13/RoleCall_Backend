package com.rolecall.auth.service;

import com.rolecall.auth.entity.RefreshToken;
import com.rolecall.auth.exception.InvalidRefreshTokenException;
import com.rolecall.auth.repository.RefreshTokenRepository;
import com.rolecall.auth.security.jwt.JwtKeyProvider;
import com.rolecall.auth.security.jwt.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() throws Exception {
        JwtKeyProvider keyProvider = new JwtKeyProvider();
        keyProvider.init();
        jwtTokenService = new JwtTokenService(keyProvider, 15);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtTokenService, 30L);
    }

    @Test
    void rotatingAValidTokenRevokesItAndIssuesAReplacement() {
        UUID userId = UUID.randomUUID();
        String rawToken = jwtTokenService.generateRawRefreshToken();
        RefreshToken existing = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(jwtTokenService.hashRefreshToken(rawToken))
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(existing.getTokenHash())).thenReturn(Optional.of(existing));

        RefreshTokenService.IssuedRefreshToken next = refreshTokenService.rotate(rawToken);

        assertThat(next.userId()).isEqualTo(userId);
        assertThat(existing.isRevoked()).isTrue();
        assertThat(existing.getReplacedByTokenId()).isEqualTo(next.tokenId());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // new token + revoked old one
    }

    @Test
    void replayingAnAlreadyRevokedTokenKillsTheWholeFamily() {
        UUID userId = UUID.randomUUID();
        String rawToken = jwtTokenService.generateRawRefreshToken();
        RefreshToken revoked = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(jwtTokenService.hashRefreshToken(rawToken))
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenHash(revoked.getTokenHash())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).revokeAllForUser(userId);
    }

    @Test
    void rotatingAnExpiredTokenFails() {
        String rawToken = jwtTokenService.generateRawRefreshToken();
        RefreshToken expired = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .tokenHash(jwtTokenService.hashRefreshToken(rawToken))
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(expired.getTokenHash())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotatingAnUnknownTokenFails() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
