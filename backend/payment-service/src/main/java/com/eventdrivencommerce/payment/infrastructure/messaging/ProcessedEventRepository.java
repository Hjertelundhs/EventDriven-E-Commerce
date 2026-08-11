package com.eventdrivencommerce.payment.infrastructure.messaging;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventJpaEntity,ProcessedEventId>{}
