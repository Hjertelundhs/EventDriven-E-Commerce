package com.eventdrivencommerce.product.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.eventdrivencommerce.product.application.event.ProductChangedV1;
import com.eventdrivencommerce.product.application.port.out.ProductEventOutbox;
import org.springframework.stereotype.Component;

@Component
class JpaProductEventOutbox implements ProductEventOutbox {

    private final SpringDataOutboxRepository repository;
    private final ObjectMapper objectMapper;

    JpaProductEventOutbox(SpringDataOutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(ProductChangedV1 event) {
        repository.save(OutboxJpaEntity.pending(
                event.eventId(), event.aggregateId(), event.eventType(), event.eventVersion(),
                event.correlationId(), event.causationId(), event.occurredAt(), objectMapper.valueToTree(event)
        ));
    }
}
