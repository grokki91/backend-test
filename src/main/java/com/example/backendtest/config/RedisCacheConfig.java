package com.example.backendtest.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.example.backendtest.user.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Cache-aside entries expire, so stale data and invalidation are observable.
 *
 * <p>The cache holds the public {@link UserResponse} view, never the entity: caching the
 * entity would put the password hash in Redis. Because the stored type is fixed, the
 * serializer is bound to it and needs no polymorphic type information.
 */
@Configuration
public class RedisCacheConfig {

    @Bean
    RedisCacheConfiguration cacheConfiguration(@Value("${app.cache.ttl-seconds}") long ttlSeconds) {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new Jackson2JsonRedisSerializer<>(mapper, UserResponse.class)));
    }
}
