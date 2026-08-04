package com.eventdrivencommerce.inventory.application.port.out;

import com.eventdrivencommerce.inventory.domain.model.InventoryReservation;
import com.eventdrivencommerce.inventory.domain.model.Sku;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

    InventoryReservation save(InventoryReservation reservation);

    Optional<InventoryReservation> findById(UUID reservationId);

    Optional<InventoryReservation> findByOrderIdAndSku(UUID orderId, Sku sku);
}
