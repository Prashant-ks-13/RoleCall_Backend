package com.rolecall.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * auth-service has no authenticated principal to audit against (every endpoint is
 * public), so the useful audit trail here is simply which client hit which
 * credential-handling endpoint and what came back.
 */
public class RequestAuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestAuditFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
        log.info("auth request method={} path={} clientIp={} status={}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), response.getStatus());
    }
}
