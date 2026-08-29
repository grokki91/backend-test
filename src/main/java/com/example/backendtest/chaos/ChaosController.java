package com.example.backendtest.chaos;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backendtest.common.ApiException;

/**
 * Deliberate misbehaviour on demand: slow responses, chosen status codes, flaky
 * endpoints and oversized payloads. This is what client timeouts, retry policies and
 * circuit breakers get tested against.
 */
@RestController
@RequestMapping("/api/v1/chaos")
public class ChaosController {

    private static final long MAX_DELAY_MILLIS = 60_000L;

    /** Sleeps for the requested time, so a client-side read timeout can be proven. */
    @GetMapping("/slow")
    public Map<String, Object> slow(@RequestParam(defaultValue = "1000") long ms) throws InterruptedException {
        if (ms < 0 || ms > MAX_DELAY_MILLIS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ms must be between 0 and " + MAX_DELAY_MILLIS);
        }
        Thread.sleep(ms);
        return Map.of("sleptMs", ms);
    }

    /** Answers with whatever status you ask for. */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestParam(defaultValue = "500") int code) {
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown status code: " + code);
        }
        return ResponseEntity.status(status).body(Map.of("requestedStatus", code));
    }

    /** Fails a share of calls at random — the case retry logic exists for. */
    @GetMapping("/flaky")
    public Map<String, Object> flaky(@RequestParam(defaultValue = "0.5") double failureRate) {
        if (failureRate < 0 || failureRate > 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "failureRate must be between 0 and 1");
        }
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Randomly failed, this is the point");
        }
        return Map.of("ok", true);
    }

    /** Returns a payload of the requested size, for streaming and limit testing. */
    @GetMapping("/payload")
    public Map<String, Object> payload(@RequestParam(defaultValue = "1024") int bytes) {
        if (bytes < 0 || bytes > 5_000_000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "bytes must be between 0 and 5000000");
        }
        return Map.of("size", bytes, "data", "x".repeat(bytes));
    }
}
