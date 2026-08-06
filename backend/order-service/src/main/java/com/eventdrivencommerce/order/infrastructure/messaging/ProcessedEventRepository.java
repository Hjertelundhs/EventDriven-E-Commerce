package com.eventdrivencommerce.order.infrastructure.messaging;
import org.springframework.data.jpa.repository.JpaRepository;
interface ProcessedEventRepository extends JpaRepository<ProcessedEventJpaEntity,ProcessedEventId>{}
