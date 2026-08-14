package com.rolecall.auth.security.jwt;

import com.nimbusds.jwt.SignedJWT;
import com.rolecall.auth.entity.User;
import com.rolecall.auth.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() throws Exception {
        JwtKeyProvider keyProvider = new JwtKeyProvider();
        keyProvider.init(); // no keystore configured -> generates an ephemeral keypair
        jwtTokenService = new JwtTokenService(keyProvider);
    }

    @Test
    void issuedAccessTokenCarriesUserClaimsAndIsRs256Signed() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("candidate@example.com")
                .role(UserRole.CANDIDATE)
                .createdAt(Instant.now())
                .build();

        String token = jwtTokenService.issueAccessToken(user);
        SignedJWT parsed = SignedJWT.parse(token);

        assertThat(parsed.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
        assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo(user.getId().toString());
        assertThat(parsed.getJWTClaimsSet().getStringClaim("email")).isEqualTo("candidate@example.com");
        assertThat(parsed.getJWTClaimsSet().getStringClaim("role")).isEqualTo("CANDIDATE");
        assertThat(parsed.getJWTClaimsSet().getExpirationTime()).isAfter(Instant.now().plusSeconds(60));
    }

    @Test
    void refreshTokenHashIsDeterministicAndDoesNotLeakTheRawValue() {
        String raw = jwtTokenService.generateRawRefreshToken();
        String hash1 = jwtTokenService.hashRefreshToken(raw);
        String hash2 = jwtTokenService.hashRefreshToken(raw);

        assertThat(hash1).isEqualTo(hash2).isNotEqualTo(raw);
        assertThat(hash1).hasSize(64); // SHA-256 hex
    }
}
