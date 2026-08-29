package com.example.backendtest.external;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Calls an external HTTP dependency — WireMock in this setup. Wrapped in a retry and a
 * circuit breaker so the fallback path is reachable from a test that stubs failures.
 */
@Component
public class ShippingClient {

    private static final Logger log = LoggerFactory.getLogger(ShippingClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public ShippingClient(RestClient restClient, @Value("${app.shipping.base-url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    @Retry(name = "shipping")
    @CircuitBreaker(name = "shipping", fallbackMethod = "quoteFallback")
    @SuppressWarnings("unchecked")
    public Map<String, Object> quote(String destination) {
        return restClient.get()
                .uri(baseUrl + "/shipping/quote?destination={destination}", destination)
                .retrieve()
                .body(Map.class);
    }

    private Map<String, Object> quoteFallback(String destination, Throwable error) {
        log.warn("shipping quote for {} fell back: {}", destination, error.getMessage());
        return Map.of("destination", destination, "cost", 0, "degraded", true, "reason", error.getClass()
                .getSimpleName());
    }
}
