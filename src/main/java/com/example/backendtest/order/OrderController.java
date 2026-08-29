package com.example.backendtest.order;

import java.net.URI;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.backendtest.auth.AuthInterceptor;
import com.example.backendtest.common.ApiException;
import com.example.backendtest.common.PageResponse;
import com.example.backendtest.messaging.EventBroadcaster;
import com.example.backendtest.messaging.NotificationConsumer;
import com.example.backendtest.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final long SSE_TIMEOUT_MILLIS = 300_000L;

    private final OrderService orderService;
    private final EventBroadcaster broadcaster;
    private final NotificationConsumer notifications;

    public OrderController(OrderService orderService, EventBroadcaster broadcaster,
                           NotificationConsumer notifications) {
        this.orderService = orderService;
        this.broadcaster = broadcaster;
        this.notifications = notifications;
    }

    @GetMapping
    public PageResponse<OrderResponse> list(@RequestParam(required = false) OrderStatus status,
                                            @RequestAttribute(AuthInterceptor.USER_ID) Long callerId,
                                            @RequestAttribute(AuthInterceptor.ROLE) String role,
                                            @ParameterObject @PageableDefault(sort = "id",
                                                    direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(orderService.list(callerId, role, status, pageable), OrderResponse::from);
    }

    /** Live feed of order events over Server-Sent Events. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return broadcaster.openSseStream(SSE_TIMEOUT_MILLIS);
    }

    /** What the RabbitMQ worker actually received — the assertion point for async delivery. */
    @GetMapping("/notifications")
    public List<OrderEvent> deliveredNotifications() {
        return notifications.delivered();
    }

    @GetMapping("/subscribers")
    public SubscriberCount subscribers() {
        return new SubscriberCount(broadcaster.sseSubscribers(), broadcaster.webSocketSubscribers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id,
                                                 @RequestAttribute(AuthInterceptor.USER_ID) Long callerId,
                                                 @RequestAttribute(AuthInterceptor.ROLE) String role) {
        Order order = orderService.get(id, callerId, role);
        return ResponseEntity.ok().eTag("\"" + order.getVersion() + "\"").body(OrderResponse.from(order));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequests.Create request,
                                                @RequestHeader(value = "Idempotency-Key", required = false)
                                                String idempotencyKey,
                                                @RequestAttribute(AuthInterceptor.USER_ID) Long callerId) {
        OrderService.CreateResult result = orderService.create(callerId, request, idempotencyKey);
        OrderResponse body = OrderResponse.from(result.order());
        if (result.replayed()) {
            return ResponseEntity.ok().header("Idempotency-Replayed", "true").body(body);
        }
        return ResponseEntity.created(URI.create("/api/v1/orders/" + result.order().getId()))
                .eTag("\"" + result.order().getVersion() + "\"")
                .body(body);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> patch(@PathVariable Long id,
                                               @Valid @RequestBody OrderRequests.Patch request,
                                               @RequestHeader(value = HttpHeaders.IF_MATCH, required = false)
                                               String ifMatch,
                                               @RequestAttribute(AuthInterceptor.USER_ID) Long callerId,
                                               @RequestAttribute(AuthInterceptor.ROLE) String role) {
        Order order = orderService.patch(id, request, parseIfMatch(ifMatch), callerId, role);
        return ResponseEntity.ok().eTag("\"" + order.getVersion() + "\"").body(OrderResponse.from(order));
    }

    @PostMapping("/{id}/pay")
    public OrderResponse pay(@PathVariable Long id,
                             @RequestAttribute(AuthInterceptor.USER_ID) Long callerId,
                             @RequestAttribute(AuthInterceptor.ROLE) String role) {
        return OrderResponse.from(orderService.transition(id, OrderStatus.PAID, callerId, role));
    }

    @PostMapping("/{id}/ship")
    public OrderResponse ship(@PathVariable Long id,
                              @RequestAttribute(AuthInterceptor.USER_ID) Long callerId,
                              @RequestAttribute(AuthInterceptor.ROLE) String role) {
        requireAdmin(role);
        return OrderResponse.from(orderService.transition(id, OrderStatus.SHIPPED, callerId, role));
    }

    @PostMapping("/{id}/deliver")
    public OrderResponse deliver(@PathVariable Long id,
                                 @RequestAttribute(AuthInterceptor.USER_ID) Long callerId,
                                 @RequestAttribute(AuthInterceptor.ROLE) String role) {
        requireAdmin(role);
        return OrderResponse.from(orderService.transition(id, OrderStatus.DELIVERED, callerId, role));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id,
                                @RequestAttribute(AuthInterceptor.USER_ID) Long callerId,
                                @RequestAttribute(AuthInterceptor.ROLE) String role) {
        return OrderResponse.from(orderService.transition(id, OrderStatus.CANCELLED, callerId, role));
    }

    /** Publishes an event both consumers reject, so retry and DLQ behaviour can be observed. */
    @PostMapping("/{id}/poison")
    public ResponseEntity<Void> poison(@PathVariable Long id,
                                       @RequestAttribute(AuthInterceptor.USER_ID) Long callerId,
                                       @RequestAttribute(AuthInterceptor.ROLE) String role) {
        requireAdmin(role);
        orderService.emitPoisonEvent(id, callerId, role);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestAttribute(AuthInterceptor.USER_ID) Long callerId,
                                       @RequestAttribute(AuthInterceptor.ROLE) String role) {
        requireAdmin(role);
        orderService.delete(id, callerId, role);
        return ResponseEntity.noContent().build();
    }

    private Long parseIfMatch(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return null;
        }
        String value = ifMatch.trim().replace("W/", "").replace("\"", "");
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "If-Match must be a version ETag such as \"3\", got: " + ifMatch);
        }
    }

    private void requireAdmin(String role) {
        if (!User.ROLE_ADMIN.equals(role)) {
            throw ApiException.forbidden("Requires role ADMIN");
        }
    }

    public record SubscriberCount(int sse, int websocket) {
    }
}
