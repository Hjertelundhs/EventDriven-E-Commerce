package com.eventdrivencommerce.inventory.infrastructure.persistence;

import com.eventdrivencommerce.inventory.domain.model.InventoryReservation;
import com.eventdrivencommerce.inventory.domain.model.Sku;

final class ReservationPersistenceMapper {

    private ReservationPersistenceMapper() {}

    static InventoryReservation toDomain(InventoryReservationJpaEntity entity) {
        return InventoryReservation.rehydrate(entity.getId(), entity.getInventoryItemId(), entity.getOrderId(),
                new Sku(entity.getSku()), entity.getQuantity(), entity.getStatus(), entity.getCreatedAt(),
                entity.getUpdatedAt(), entity.getVersion());
    }

    static void copyToEntity(InventoryReservation reservation, InventoryReservationJpaEntity entity) {
        entity.setInventoryItemId(reservation.inventoryItemId());
        entity.setOrderId(reservation.orderId());
        entity.setSku(reservation.sku().value());
        entity.setQuantity(reservation.quantity());
        entity.setStatus(reservation.status());
        entity.setCreatedAt(reservation.createdAt());
        entity.setUpdatedAt(reservation.updatedAt());
    }
}
