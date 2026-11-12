package com.rolecall.gateway.fallback;

import com.rolecall.common.error.ApiError;
import com.rolecall.common.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Hit when a route's circuit breaker is open or the downstream call times out
 * (see the CircuitBreaker gateway filter configured on each route). No method
 * restriction: the forward preserves whatever HTTP method the original request used.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/{service}")
    public Mono<ResponseEntity<ApiError>> fallback(@PathVariable String service) {
        ApiError body = ApiError.of(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCode.INTERNAL_ERROR,
                service + " is temporarily unavailable, please retry shortly", "/fallback/" + service);
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
