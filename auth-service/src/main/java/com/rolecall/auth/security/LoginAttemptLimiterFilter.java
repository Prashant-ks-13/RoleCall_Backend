package com.rolecall.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolecall.common.error.ApiError;
import com.rolecall.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-client-IP fixed-window limiter on the endpoints that create or spend a
 * session (login/register) - the point where brute-force/credential-stuffing
 * actually matters for this service. In-memory, so this resets per instance
 * restart and does not share state across replicas; fine for the single
 * auth-service instance this backend currently runs.
 */
public class LoginAttemptLimiterFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of("/api/auth/login", "/api/auth/register");
    private static final int MAX_ATTEMPTS_PER_WINDOW = 10;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!LIMITED_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = clientIp(request);
        if (!tryConsume(clientIp)) {
            reject(response, request.getRequestURI());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean tryConsume(String clientIp) {
        Window window = windows.computeIfAbsent(clientIp, ip -> new Window(Instant.now()));
        if (Instant.now().isAfter(window.windowStart.plus(WINDOW))) {
            windows.put(clientIp, new Window(Instant.now()));
            window = windows.get(clientIp);
        }
        return window.count.incrementAndGet() <= MAX_ATTEMPTS_PER_WINDOW;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response, String path) throws IOException {
        ApiError body = ApiError.of(HttpStatus.TOO_MANY_REQUESTS.value(), ErrorCode.BAD_REQUEST,
                "Too many attempts, please try again later", path);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private static final class Window {
        private final Instant windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        private Window(Instant windowStart) {
            this.windowStart = windowStart;
        }
    }
}
