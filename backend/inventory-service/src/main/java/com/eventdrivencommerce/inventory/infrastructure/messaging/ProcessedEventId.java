package com.eventdrivencommerce.inventory.infrastructure.messaging;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
@Embeddable record ProcessedEventId(String consumerGroup, UUID eventId) implements Serializable {}
