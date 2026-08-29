package com.example.backendtest.order;

import java.time.Instant;

import com.example.backendtest.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Remembers the answer given for an Idempotency-Key so a retried POST returns the
 * original result instead of creating a second order.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key")
    private String key;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_fingerprint", nullable = false)
    private String requestFingerprint;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Timestamps.now();

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String key, Long userId, String requestFingerprint, Long orderId) {
        this.key = key;
        this.userId = userId;
        this.requestFingerprint = requestFingerprint;
        this.orderId = orderId;
    }

    public String getKey() {
        return key;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
