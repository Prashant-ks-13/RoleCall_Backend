package com.rolecall.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Logs an audit trail for every request the gateway's security chain let through,
 * once GatewaySecurityConfig has already made the allow decision.
 */
@Component
public class AuthorizationAuditGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationAuditGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .doOnNext(authentication -> logDecision(exchange, authentication))
                .then(chain.filter(exchange))
                .switchIfEmpty(chain.filter(exchange));
    }

    private void logDecision(ServerWebExchange exchange, org.springframework.security.core.Authentication authentication) {
        String userId = "anonymous";
        String role = "none";
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            userId = jwtAuth.getToken().getSubject();
            role = jwtAuth.getToken().getClaimAsString("role");
        }
        log.info("authorized method={} path={} userId={} role={}",
                exchange.getRequest().getMethod(), exchange.getRequest().getPath(), userId, role);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
