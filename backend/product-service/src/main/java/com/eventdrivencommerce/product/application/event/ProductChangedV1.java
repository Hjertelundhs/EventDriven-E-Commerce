package com.eventdrivencommerce.product.application.event;

import com.eventdrivencommerce.product.domain.model.Product;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProductChangedV1(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID aggregateId,
        UUID correlationId,
        UUID causationId,
        Instant occurredAt,
        Payload payload
) {
    public static final String TYPE = "ProductChangedV1";
    public static final int VERSION = 1;

    public ProductChangedV1 {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(causationId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
        if (!TYPE.equals(eventType) || eventVersion != VERSION) {
            throw new IllegalArgumentException("Product event type and version must match ProductChangedV1");
        }
    }

    public static ProductChangedV1 from(Product product, ChangeType changeType,
                                        UUID correlationId, UUID causationId, Instant occurredAt) {
        return new ProductChangedV1(
                UUID.randomUUID(), TYPE, VERSION, product.id(), correlationId, causationId, occurredAt,
                new Payload(product.id(), product.sku().value(), changeType, product.version(), occurredAt)
        );
    }

    public record Payload(UUID productId, String sku, ChangeType changeType, long productVersion, Instant changedAt) {}

    public enum ChangeType { CREATED, UPDATED, DEACTIVATED }
}
