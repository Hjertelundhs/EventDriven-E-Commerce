package com.eventdrivencommerce.product.infrastructure.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "product.outbox.publisher-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisher.class);

    private final SpringDataOutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxPublisherProperties properties;
    private final Clock clock;

    public OutboxPublisher(SpringDataOutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate,
                           OutboxPublisherProperties properties, Clock clock) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${product.outbox.poll-interval:PT1S}")
    @Transactional
    public void publishPending() {
        for (OutboxJpaEntity event : repository.lockPendingBatch(properties.batchSize())) {
            Instant now = clock.instant();
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        properties.topic(), event.getAggregateId().toString(), event.getPayload().toString());
                addHeader(record, "eventId", event.getEventId().toString());
                addHeader(record, "eventType", event.getEventType());
                addHeader(record, "eventVersion", Integer.toString(event.getEventVersion()));
                addHeader(record, "correlationId", event.getCorrelationId().toString());
                addHeader(record, "causationId", event.getCausationId().toString());
                addHeader(record, "contentType", "application/json");
                addHeader(record, "schema", "product-changed-v1.schema.json");
                addHeader(record, "producer", "product-service");
                kafkaTemplate.send(record).get(properties.publishTimeout().toMillis(), TimeUnit.MILLISECONDS);
                event.markPublished(now);
            } catch (Exception exception) {
                event.markFailed(now, rootMessage(exception));
                LOGGER.warn("Product outbox publication failed eventId={} aggregateId={}",
                        event.getEventId(), event.getAggregateId());
            }
        }
    }

    private static void addHeader(ProducerRecord<String, String> record, String name, String value) {
        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }

    private static String rootMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
