package com.example.backendtest.order;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.backendtest.common.Timestamps;
import java.util.UUID;

/** What travels through Kafka and RabbitMQ whenever an order changes state. */
public record OrderEvent(String eventId, String type, Long orderId, Long userId, OrderStatus status,
                         BigDecimal amount, String currency, String correlationId, Instant occurredAt) {

    public static OrderEvent of(String type, Order order, String correlationId) {
        return new OrderEvent(UUID.randomUUID().toString(), type, order.getId(), order.getUserId(), order.getStatus(),
                order.getAmount(), order.getCurrency(), correlationId, Timestamps.now());
    }
}
