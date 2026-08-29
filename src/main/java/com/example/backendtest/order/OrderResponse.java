package com.example.backendtest.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, Long userId, OrderStatus status, BigDecimal amount, String currency,
                            String comment, Long version, List<OrderStatus> allowedTransitions, Instant createdAt,
                            Instant updatedAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getUserId(), order.getStatus(), order.getAmount(),
                order.getCurrency(), order.getComment(), order.getVersion(),
                List.copyOf(order.getStatus().allowedNext()), order.getCreatedAt(), order.getUpdatedAt());
    }
}
