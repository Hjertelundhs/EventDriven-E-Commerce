package com.eventdrivencommerce.product.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
class OutboxJpaEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "causation_id", nullable = false)
    private UUID causationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OutboxJpaEntity() {}

    static OutboxJpaEntity pending(UUID eventId, UUID aggregateId, String eventType, int eventVersion,
                                   UUID correlationId, UUID causationId, Instant occurredAt, JsonNode payload) {
        OutboxJpaEntity entity = new OutboxJpaEntity();
        entity.eventId = eventId;
        entity.aggregateId = aggregateId;
        entity.eventType = eventType;
        entity.eventVersion = eventVersion;
        entity.correlationId = correlationId;
        entity.causationId = causationId;
        entity.occurredAt = occurredAt;
        entity.payload = payload;
        entity.status = OutboxStatus.PENDING;
        entity.attempts = 0;
        entity.nextAttemptAt = occurredAt;
        entity.createdAt = occurredAt;
        return entity;
    }

    void markPublished(Instant now) {
        status = OutboxStatus.PUBLISHED;
        publishedAt = now;
        lastError = null;
    }

    void markFailed(Instant now, String error) {
        attempts++;
        long delaySeconds = Math.min(60, 1L << Math.min(attempts, 6));
        nextAttemptAt = now.plusSeconds(delaySeconds);
        lastError = error == null ? "Unknown publication failure" : error.substring(0, Math.min(500, error.length()));
    }

    UUID getEventId() { return eventId; }
    UUID getAggregateId() { return aggregateId; }
    String getEventType() { return eventType; }
    int getEventVersion() { return eventVersion; }
    UUID getCorrelationId() { return correlationId; }
    UUID getCausationId() { return causationId; }
    JsonNode getPayload() { return payload; }
}
