package com.example.backendtest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.backendtest.auth.AuthInterceptor;
import com.example.backendtest.common.RateLimitInterceptor;

/**
 * Auth runs first so the rate limiter can bucket by user rather than by IP.
 * Everything under /api/v1 and /api/v2 is rate limited; only the listed paths need a token.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, RateLimitInterceptor rateLimitInterceptor) {
        this.authInterceptor = authInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/api/v1/auth/me",
                        "/api/v1/users", "/api/v1/users/**",
                        "/api/v2/users", "/api/v2/users/**",
                        "/api/v1/orders", "/api/v1/orders/**",
                        "/api/v1/webhooks", "/api/v1/webhooks/**");
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/**", "/api/v2/**");
    }
}
