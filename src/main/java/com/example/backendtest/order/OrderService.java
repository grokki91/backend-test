package com.example.backendtest.order;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backendtest.common.ApiException;
import com.example.backendtest.common.CorrelationIdFilter;
import com.example.backendtest.messaging.OrderEventPublisher;
import com.example.backendtest.user.User;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final IdempotencyRepository idempotencyKeys;
    private final OrderEventPublisher events;

    public OrderService(OrderRepository orders, IdempotencyRepository idempotencyKeys, OrderEventPublisher events) {
        this.orders = orders;
        this.idempotencyKeys = idempotencyKeys;
        this.events = events;
    }

    public Page<Order> list(Long callerId, String role, OrderStatus status, Pageable pageable) {
        boolean admin = User.ROLE_ADMIN.equals(role);
        if (admin) {
            return status == null ? orders.findAll(pageable) : orders.findByStatus(status, pageable);
        }
        return status == null
                ? orders.findByUserId(callerId, pageable)
                : orders.findByUserIdAndStatus(callerId, status, pageable);
    }

    /** Reading someone else's order is the IDOR case: it answers 403, not 404. */
    public Order get(Long id, Long callerId, String role) {
        Order order = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order", id));
        if (!User.ROLE_ADMIN.equals(role) && !order.getUserId().equals(callerId)) {
            throw ApiException.forbidden("This order belongs to another user");
        }
        return order;
    }

    /**
     * Creates an order, honouring an Idempotency-Key: the same key with the same body
     * replays the first order, the same key with a different body is a conflict.
     */
    @Transactional
    public CreateResult create(Long userId, OrderRequests.Create request, String idempotencyKey) {
        String fingerprint = fingerprint(userId, request);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyRecord> existing = idempotencyKeys.findById(idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();
                if (!record.getRequestFingerprint().equals(fingerprint)) {
                    throw ApiException.conflict("Idempotency-Key already used with a different payload");
                }
                Order replayed = orders.findById(record.getOrderId())
                        .orElseThrow(() -> ApiException.notFound("Order", record.getOrderId()));
                return new CreateResult(replayed, true);
            }
        }

        Order order = orders.save(new Order(userId, request.amount(), request.currency(), request.comment()));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyKeys.save(new IdempotencyRecord(idempotencyKey, userId, fingerprint, order.getId()));
        }
        events.publish(OrderEvent.of("order.created", order, correlationId()));
        return new CreateResult(order, false);
    }

    /**
     * Partial update, guarded twice: an If-Match that no longer matches fails fast with 412,
     * and a concurrent writer that slips past it loses on the JPA version check with 409.
     */
    @Transactional
    public Order patch(Long id, OrderRequests.Patch request, Long expectedVersion, Long callerId, String role) {
        Order order = get(id, callerId, role);
        if (expectedVersion != null && !expectedVersion.equals(order.getVersion())) {
            throw new ApiException(HttpStatus.PRECONDITION_FAILED,
                    "If-Match version %d is stale, current is %d".formatted(expectedVersion, order.getVersion()));
        }
        if (order.getStatus() != OrderStatus.NEW) {
            throw ApiException.conflict("Only a NEW order can be edited, this one is " + order.getStatus());
        }
        if (request.amount() != null) {
            order.setAmount(request.amount());
        }
        if (request.currency() != null) {
            order.setCurrency(request.currency());
        }
        if (request.comment() != null) {
            order.setComment(request.comment());
        }
        Order saved = orders.save(order);
        events.publish(OrderEvent.of("order.updated", saved, correlationId()));
        return saved;
    }

    @Transactional
    public Order transition(Long id, OrderStatus target, Long callerId, String role) {
        Order order = get(id, callerId, role);
        if (order.getStatus() == target) {
            throw ApiException.conflict("Order is already " + target);
        }
        if (!order.getStatus().canMoveTo(target)) {
            throw ApiException.conflict("Cannot move order from %s to %s".formatted(order.getStatus(), target));
        }
        order.setStatus(target);
        Order saved = orders.save(order);
        events.publish(OrderEvent.of("order." + target.name().toLowerCase(), saved, correlationId()));
        return saved;
    }

    @Transactional
    public void delete(Long id, Long callerId, String role) {
        orders.delete(get(id, callerId, role));
    }

    /** Emits an event the consumers always reject, so retries and the DLQ are observable. */
    public void emitPoisonEvent(Long id, Long callerId, String role) {
        events.publish(OrderEvent.of("order.poison", get(id, callerId, role), correlationId()));
    }

    private String fingerprint(Long userId, OrderRequests.Create request) {
        String raw = "%d|%s|%s|%s".formatted(userId, request.amount(), request.currency(), request.comment());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot fingerprint request", e);
        }
    }

    private String correlationId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }

    /** {@code replayed} tells the controller to answer 200 instead of 201. */
    public record CreateResult(Order order, boolean replayed) {
    }
}
