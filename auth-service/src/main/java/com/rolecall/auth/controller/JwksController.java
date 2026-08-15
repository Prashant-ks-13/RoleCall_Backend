package com.rolecall.auth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.rolecall.auth.security.jwt.JwtKeyProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "JWKS", description = "Public key set used by other services to verify access tokens")
public class JwksController {

    private final JwtKeyProvider keyProvider;

    public JwksController(JwtKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "Public JWK Set for verifying access tokens issued by this service")
    public Map<String, Object> jwks() {
        return new JWKSet(keyProvider.getRsaJwk().toPublicJWK()).toJSONObject();
    }
}
