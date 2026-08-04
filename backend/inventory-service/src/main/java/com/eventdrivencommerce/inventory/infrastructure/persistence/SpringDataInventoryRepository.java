package com.eventdrivencommerce.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataInventoryRepository extends JpaRepository<InventoryItemJpaEntity, UUID> {

    Optional<InventoryItemJpaEntity> findBySku(String sku);
}
