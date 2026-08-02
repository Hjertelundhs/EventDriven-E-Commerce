package com.eventdrivencommerce.product.infrastructure.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "product.outbox")
public record OutboxPublisherProperties(String topic, int batchSize, Duration publishTimeout) {
    public OutboxPublisherProperties {
        topic = topic == null || topic.isBlank() ? "commerce.product.v1" : topic;
        batchSize = batchSize < 1 ? 50 : batchSize;
        publishTimeout = publishTimeout == null ? Duration.ofSeconds(10) : publishTimeout;
    }
}
