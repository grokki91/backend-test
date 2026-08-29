package com.example.backendtest.common;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Fixed-window rate limit held in Redis, so the counter is shared across app instances.
 * Answers 429 with Retry-After and always reports the window in X-RateLimit-* headers.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redis;
    private final int limit;
    private final int windowSeconds;

    public RateLimitInterceptor(StringRedisTemplate redis,
                                @Value("${app.rate-limit.requests-per-window}") int limit,
                                @Value("${app.rate-limit.window-seconds}") int windowSeconds) {
        this.redis = redis;
        this.limit = limit;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long window = Instant.now().getEpochSecond() / windowSeconds;
        String key = "ratelimit:%s:%d".formatted(clientId(request), window);

        Long used = redis.opsForValue().increment(key);
        if (used != null && used == 1L) {
            redis.expire(key, Duration.ofSeconds(windowSeconds));
        }
        long count = used == null ? 1L : used;
        long resetAt = (window + 1) * windowSeconds;

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetAt));

        if (count > limit) {
            response.setHeader("Retry-After", String.valueOf(resetAt - Instant.now().getEpochSecond()));
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded: %d requests per %ds"
                    .formatted(limit, windowSeconds));
        }
        return true;
    }

    /** Authenticated callers get their own bucket; anonymous ones share a bucket per IP. */
    private String clientId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId != null ? "user:" + userId : "ip:" + request.getRemoteAddr();
    }
}
