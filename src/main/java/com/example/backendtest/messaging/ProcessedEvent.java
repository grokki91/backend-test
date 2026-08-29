package com.example.backendtest.messaging;

import java.time.Instant;

import com.example.backendtest.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The consumer's dedupe log. Kafka gives at-least-once delivery, so the same event can
 * arrive twice; recording the id makes the consumer effectively idempotent.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt = Timestamps.now();

    protected ProcessedEvent() {
    }

    public ProcessedEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }
}
