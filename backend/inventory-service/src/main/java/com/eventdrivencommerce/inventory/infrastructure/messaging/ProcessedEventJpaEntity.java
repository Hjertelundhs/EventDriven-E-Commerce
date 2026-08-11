package com.eventdrivencommerce.inventory.infrastructure.messaging;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="processed_events") class ProcessedEventJpaEntity {
    @EmbeddedId @AttributeOverrides({@AttributeOverride(name="consumerGroup",column=@Column(name="consumer_group")),@AttributeOverride(name="eventId",column=@Column(name="event_id"))}) ProcessedEventId id;
    @Column(name="event_type",nullable=false) String eventType; @Column(name="processed_at",nullable=false) Instant processedAt;
    protected ProcessedEventJpaEntity() {} ProcessedEventJpaEntity(ProcessedEventId id,String type,Instant at){this.id=id;this.eventType=type;this.processedAt=at;}
}
