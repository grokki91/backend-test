package com.example.backendtest.webhook;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.backendtest.order.OrderEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Posts each matching event to the subscriber's URL with an HMAC-SHA256 signature,
 * the way a real webhook sender proves the payload came from it. Point a subscription
 * at WireMock and assert what arrived.
 */
@Component
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final WebhookRepository subscriptions;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WebhookDispatcher(WebhookRepository subscriptions, RestClient restClient, ObjectMapper objectMapper) {
        this.subscriptions = subscriptions;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public void dispatch(OrderEvent event) {
        for (WebhookSubscription subscription : subscriptions.findAll()) {
            if (!subscription.matches(event.type())) {
                continue;
            }
            try {
                String payload = objectMapper.writeValueAsString(event);
                restClient.post()
                        .uri(subscription.getTargetUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Event", event.type())
                        .header("X-Webhook-Delivery", event.eventId())
                        .header("X-Webhook-Signature", "sha256=" + sign(payload, subscription.getSecret()))
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                log.info("webhook delivered to {} event={}", subscription.getTargetUrl(), event.type());
            } catch (Exception e) {
                // A failing subscriber must not break event consumption — that is the subscriber's problem.
                log.warn("webhook delivery to {} failed: {}", subscription.getTargetUrl(), e.getMessage());
            }
        }
    }

    private String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign webhook payload", e);
        }
    }
}
