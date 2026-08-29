package com.example.backendtest.config;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.backendtest.messaging.Topics;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange notificationsExchange() {
        return new TopicExchange(Topics.NOTIFICATIONS_EXCHANGE);
    }

    /** A rejected message is routed to the dead-letter exchange instead of being requeued forever. */
    @Bean
    Queue notificationsQueue() {
        return QueueBuilder.durable(Topics.NOTIFICATIONS_QUEUE)
                .withArguments(Map.of("x-dead-letter-exchange", Topics.NOTIFICATIONS_DLX))
                .build();
    }

    @Bean
    Binding notificationsBinding() {
        return BindingBuilder.bind(notificationsQueue()).to(notificationsExchange())
                .with(Topics.NOTIFICATIONS_ROUTING_KEY);
    }

    @Bean
    FanoutExchange notificationsDeadLetterExchange() {
        return new FanoutExchange(Topics.NOTIFICATIONS_DLX);
    }

    @Bean
    Queue notificationsDeadLetterQueue() {
        return QueueBuilder.durable(Topics.NOTIFICATIONS_DLQ).build();
    }

    @Bean
    Binding notificationsDeadLetterBinding() {
        return BindingBuilder.bind(notificationsDeadLetterQueue()).to(notificationsDeadLetterExchange());
    }

    @Bean
    MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
