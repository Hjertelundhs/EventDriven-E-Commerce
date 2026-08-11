package com.eventdrivencommerce.inventory.infrastructure.messaging;
import org.springframework.data.jpa.repository.JpaRepository;
interface ProcessedEventRepository extends JpaRepository<ProcessedEventJpaEntity, ProcessedEventId> {}
