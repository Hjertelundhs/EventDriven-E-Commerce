package com.eventdrivencommerce.payment.infrastructure.messaging;
import jakarta.persistence.*;import java.time.Instant;
@Entity @Table(name="processed_events")public class ProcessedEventJpaEntity{@EmbeddedId ProcessedEventId id;@Column(name="event_type",nullable=false,length=120)String eventType;@Column(name="processed_at",nullable=false)Instant processedAt;protected ProcessedEventJpaEntity(){}public ProcessedEventJpaEntity(ProcessedEventId id,String type,Instant at){this.id=id;eventType=type;processedAt=at;}}
