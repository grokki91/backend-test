package com.example.backendtest.messaging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.backendtest.order.OrderEvent;

/**
 * RabbitMQ side of the flow. Keeps the last delivered notifications in memory so a test
 * can assert what the worker saw, and rejects {@code order.poison} so it lands in the DLQ.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private static final String POISON_TYPE = "order.poison";
    private static final int KEEP_LAST = 100;

    private final List<OrderEvent> delivered = new CopyOnWriteArrayList<>();

    @RabbitListener(queues = Topics.NOTIFICATIONS_QUEUE)
    public void consume(OrderEvent event) {
        if (POISON_TYPE.equals(event.type())) {
            throw new IllegalStateException("Poison notification, routed to DLQ: " + event.eventId());
        }
        delivered.add(event);
        while (delivered.size() > KEEP_LAST) {
            delivered.remove(0);
        }
        log.info("notification delivered type={} orderId={}", event.type(), event.orderId());
    }

    public List<OrderEvent> delivered() {
        return List.copyOf(delivered);
    }
}
