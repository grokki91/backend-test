package com.example.backendtest.webhook;

import java.time.Instant;

import com.example.backendtest.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_url", nullable = false)
    private String targetUrl;

    /** Event type to match, or {@code *} for every event. */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String secret;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Timestamps.now();

    protected WebhookSubscription() {
    }

    public WebhookSubscription(String targetUrl, String eventType, String secret) {
        this.targetUrl = targetUrl;
        this.eventType = eventType;
        this.secret = secret;
    }

    public boolean matches(String type) {
        return active && ("*".equals(eventType) || eventType.equals(type));
    }

    public Long getId() {
        return id;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSecret() {
        return secret;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
