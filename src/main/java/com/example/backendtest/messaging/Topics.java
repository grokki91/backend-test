package com.example.backendtest.messaging;

/** Every queue and topic name in one place, so tests and the app agree on them. */
public final class Topics {

    public static final String ORDER_EVENTS = "orders.events";
    public static final String ORDER_EVENTS_DLT = "orders.events.DLT";

    public static final String NOTIFICATIONS_EXCHANGE = "notifications.exchange";
    public static final String NOTIFICATIONS_QUEUE = "notifications.queue";
    public static final String NOTIFICATIONS_ROUTING_KEY = "notifications.order";
    public static final String NOTIFICATIONS_DLQ = "notifications.queue.dlq";
    public static final String NOTIFICATIONS_DLX = "notifications.dlx";

    private Topics() {
    }
}
