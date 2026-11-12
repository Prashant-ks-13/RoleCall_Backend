package com.rolecall.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolecall.common.error.ApiError;
import com.rolecall.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Once a request has been authenticated by {@code GatewaySecurityConfig}, sanity-checks
 * that the JWT carries the claims every downstream service expects (sub, role) before
 * forwarding it on. Does not propagate claims as trust headers - every downstream
 * service still independently re-validates the same bearer token.
 */
@Component
public class SessionClaimsGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SessionClaimsGlobalFilter.class);

    private final ObjectMapper objectMapper;

    public SessionClaimsGlobalFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .flatMap(authentication -> validate(exchange, chain, authentication))
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<Void> validate(ServerWebExchange exchange, GatewayFilterChain chain, Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return chain.filter(exchange);
        }
        Jwt jwt = jwtAuth.getToken();
        String subject = jwt.getSubject();
        String role = jwt.getClaimAsString("role");
        if (subject == null || subject.isBlank() || role == null || role.isBlank()) {
            log.warn("Rejecting request to {}: JWT missing required session claims", exchange.getRequest().getPath());
            return reject(exchange);
        }
        return chain.filter(exchange);
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        ApiError body = ApiError.of(HttpStatus.UNAUTHORIZED.value(), ErrorCode.UNAUTHORIZED,
                "Session token is missing required claims", exchange.getRequest().getPath().value());
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"message\":\"Session token is missing required claims\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
