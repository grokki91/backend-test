package com.example.backendtest.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.backendtest.order.OrderEvent;

/**
 * Publishes each order event twice on purpose: to Kafka (the event log other services
 * would read) and to RabbitMQ (the work queue a notification worker would drain).
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, OrderEvent> kafka;
    private final RabbitTemplate rabbit;

    public OrderEventPublisher(KafkaTemplate<String, OrderEvent> kafka, RabbitTemplate rabbit) {
        this.kafka = kafka;
        this.rabbit = rabbit;
    }

    public void publish(OrderEvent event) {
        // Keying by order id keeps one order's events in a single partition, so their order is preserved.
        kafka.send(Topics.ORDER_EVENTS, String.valueOf(event.orderId()), event);
        rabbit.convertAndSend(Topics.NOTIFICATIONS_EXCHANGE, Topics.NOTIFICATIONS_ROUTING_KEY, event);
        log.info("published event type={} orderId={} eventId={}", event.type(), event.orderId(), event.eventId());
    }
}
