package com.example.backendtest.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.backendtest.order.OrderEvent;
import com.example.backendtest.webhook.WebhookDispatcher;

/**
 * Kafka side of the flow. Deduplicates by event id, then fans the event out to the
 * live streams and to any registered webhook.
 *
 * <p>An event whose type is {@code order.poison} always throws, so retries and the
 * dead-letter topic are something you can watch happen.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private static final String POISON_TYPE = "order.poison";

    private final ProcessedEventRepository processedEvents;
    private final EventBroadcaster broadcaster;
    private final WebhookDispatcher webhooks;

    public OrderEventConsumer(ProcessedEventRepository processedEvents, EventBroadcaster broadcaster,
                              WebhookDispatcher webhooks) {
        this.processedEvents = processedEvents;
        this.broadcaster = broadcaster;
        this.webhooks = webhooks;
    }

    @Transactional
    @KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(OrderEvent event) {
        if (POISON_TYPE.equals(event.type())) {
            throw new IllegalStateException("Poison event, will be retried then dead-lettered: " + event.eventId());
        }
        if (processedEvents.existsById(event.eventId())) {
            log.info("duplicate event ignored eventId={}", event.eventId());
            return;
        }
        processedEvents.save(new ProcessedEvent(event.eventId(), event.type()));
        broadcaster.broadcast(event);
        webhooks.dispatch(event);
        log.info("consumed event type={} orderId={} eventId={}", event.type(), event.orderId(), event.eventId());
    }
}
