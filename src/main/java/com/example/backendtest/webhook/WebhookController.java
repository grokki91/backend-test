package com.example.backendtest.webhook;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backendtest.common.ApiException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookRepository subscriptions;

    public WebhookController(WebhookRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @GetMapping
    public List<SubscriptionResponse> list() {
        return subscriptions.findAll().stream().map(SubscriptionResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> subscribe(@Valid @RequestBody SubscribeRequest request) {
        WebhookSubscription saved = subscriptions.save(
                new WebhookSubscription(request.targetUrl(), request.eventType(), request.secret()));
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unsubscribe(@PathVariable Long id) {
        WebhookSubscription subscription = subscriptions.findById(id)
                .orElseThrow(() -> ApiException.notFound("Webhook subscription", id));
        subscriptions.delete(subscription);
        return ResponseEntity.noContent().build();
    }

    public record SubscribeRequest(
            @NotBlank @Pattern(regexp = "^https?://.+", message = "must be an http(s) URL") String targetUrl,
            @NotBlank String eventType,
            @NotBlank String secret) {
    }

    public record SubscriptionResponse(Long id, String targetUrl, String eventType, boolean active) {

        static SubscriptionResponse from(WebhookSubscription subscription) {
            return new SubscriptionResponse(subscription.getId(), subscription.getTargetUrl(),
                    subscription.getEventType(), subscription.isActive());
        }
    }
}
