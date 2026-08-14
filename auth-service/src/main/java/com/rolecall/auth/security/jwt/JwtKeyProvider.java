package com.rolecall.auth.security.jwt;

import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * Provides the RSA signing keypair used to issue access tokens.
 *
 * <p>If {@code jwt.keystore.path} points to a readable PKCS12 keystore, the
 * keypair is loaded from it (durable across restarts — required for JWKS
 * consumers whose cache shouldn't be invalidated on every deploy). Otherwise
 * an ephemeral in-memory keypair is generated at startup: fine for local
 * dev, but every restart invalidates previously issued tokens, so
 * production deployments must mount a real keystore.
 */
@Component
@Slf4j
@Getter
public class JwtKeyProvider {

    @Value("${jwt.keystore.path:}")
    private String keystorePath;

    @Value("${jwt.keystore.password:}")
    private String keystorePassword;

    @Value("${jwt.key.alias:rolecall-auth}")
    private String keyAlias;

    @Value("${jwt.key.password:}")
    private String keyPassword;

    private RSAKey rsaJwk;

    @PostConstruct
    void init() throws Exception {
        if (keystorePath != null && !keystorePath.isBlank()) {
            try {
                loadFromKeystore();
                log.info("Loaded JWT signing key '{}' from keystore {}", keyAlias, keystorePath);
                return;
            } catch (Exception e) {
                log.warn("Could not load JWT keystore from {}, falling back to an ephemeral key: {}",
                        keystorePath, e.getMessage());
            }
        }
        generateEphemeralKey();
        log.warn("No JWT keystore configured — generated an EPHEMERAL RSA keypair. " +
                "Every restart invalidates previously issued tokens. Set jwt.keystore.path for production.");
    }

    private void loadFromKeystore() throws Exception {
        Resource resource = new UrlResource(keystorePath);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = resource.getInputStream()) {
            keyStore.load(is, keystorePassword.toCharArray());
        }
        RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
        RSAPublicKey publicKey = (RSAPublicKey) keyStore.getCertificate(keyAlias).getPublicKey();
        this.rsaJwk = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(keyAlias)
                .build();
    }

    private void generateEphemeralKey() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        this.rsaJwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }
}
