package com.rolecall.auth.service;

import com.rolecall.auth.entity.RefreshToken;
import com.rolecall.auth.exception.InvalidRefreshTokenException;
import com.rolecall.auth.repository.RefreshTokenRepository;
import com.rolecall.auth.security.jwt.JwtTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Refresh tokens are rotated on every use: the presented token is revoked and
 * a new one issued in its place. If a revoked token is ever presented again
 * (a strong signal it was stolen and the legitimate client already rotated
 * past it), the entire token family for that user is revoked.
 */
@Service
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final long refreshTokenTtlDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                                JwtTokenService jwtTokenService,
                                @Value("${jwt.refresh-token.ttl-days:30}") long refreshTokenTtlDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    @Transactional
    public IssuedRefreshToken issueFor(UUID userId) {
        String raw = jwtTokenService.generateRawRefreshToken();
        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(jwtTokenService.hashRefreshToken(raw))
                .expiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);
        return new IssuedRefreshToken(raw, entity.getId(), userId);
    }

    @Transactional
    public IssuedRefreshToken rotate(String rawToken) {
        String hash = jwtTokenService.hashRefreshToken(rawToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid"));

        if (existing.isRevoked()) {
            log.warn("Revoked refresh token replayed for user {} — killing entire token family", existing.getUserId());
            refreshTokenRepository.revokeAllForUser(existing.getUserId());
            throw new InvalidRefreshTokenException("Refresh token has already been used; all sessions were revoked");
        }
        if (existing.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        IssuedRefreshToken next = issueFor(existing.getUserId());
        existing.setRevoked(true);
        existing.setReplacedByTokenId(next.tokenId());
        refreshTokenRepository.save(existing);
        return next;
    }

    @Transactional
    public void revoke(String rawToken) {
        String hash = jwtTokenService.hashRefreshToken(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    public record IssuedRefreshToken(String rawToken, UUID tokenId, UUID userId) {
    }
}
