package com.example.backendtest.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Outbound HTTP with explicit, short timeouts — a hung dependency must not hang the API. */
@Configuration
public class RestClientConfig {

    @Bean
    RestClient restClient(@Value("${app.http-client.connect-timeout-ms}") long connectTimeoutMs,
                          @Value("${app.http-client.read-timeout-ms}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder().requestFactory(factory).build();
    }
}
