package com.example.backendtest.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.example.backendtest.messaging.Topics;

@Configuration
public class KafkaConfig {

    /** Three partitions, so consumer groups, assignment and per-key ordering are visible. */
    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(Topics.ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic orderEventsDeadLetterTopic() {
        return TopicBuilder.name(Topics.ORDER_EVENTS_DLT).partitions(3).replicas(1).build();
    }

    /**
     * Two retries one second apart, then the record is published to orders.events.DLT.
     * The destination is set explicitly — Spring Kafka's default would append "-dlt"
     * and quietly create a topic this app never declared.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, exception) -> new TopicPartition(Topics.ORDER_EVENTS_DLT, record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 2L));
    }
}
