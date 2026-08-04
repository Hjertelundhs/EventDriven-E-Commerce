package com.eventdrivencommerce.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataReservationRepository extends JpaRepository<InventoryReservationJpaEntity, UUID> {

    Optional<InventoryReservationJpaEntity> findByOrderIdAndSku(UUID orderId, String sku);
}
