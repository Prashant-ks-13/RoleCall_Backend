package com.rolecall.auth.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.rolecall.auth.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

@Service
public class JwtTokenService {

    private static final String ISSUER = "rolecall-auth-service";
    private final SecureRandom secureRandom = new SecureRandom();

    private final JwtKeyProvider keyProvider;
    private final long accessTokenTtlMinutes;

    public JwtTokenService(JwtKeyProvider keyProvider,
                            @Value("${jwt.access-token.ttl-minutes:15}") long accessTokenTtlMinutes) {
        this.keyProvider = keyProvider;
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public String issueAccessToken(User user) {
        try {
            Instant now = Instant.now();
            Instant expiry = now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .issuer(ISSUER)
                    .claim("email", user.getEmail())
                    .claim("role", user.getRole().name())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(keyProvider.getRsaJwk().getKeyID())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new RSASSASigner(keyProvider.getRsaJwk().toPrivateKey()));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign access token", e);
        }
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtlMinutes * 60;
    }

    /** Cryptographically random opaque refresh token, returned to the client exactly once. */
    public String generateRawRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hash of the raw refresh token — this, not the raw value, is what gets persisted. */
    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
