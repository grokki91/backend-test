package com.example.backendtest.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookRepository extends JpaRepository<WebhookSubscription, Long> {
}
