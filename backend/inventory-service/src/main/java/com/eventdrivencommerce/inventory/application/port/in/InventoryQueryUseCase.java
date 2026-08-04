package com.eventdrivencommerce.inventory.application.port.in;

import com.eventdrivencommerce.inventory.application.model.ReservationResult;
import com.eventdrivencommerce.inventory.application.model.StockResult;

import java.util.UUID;

public interface InventoryQueryUseCase {

    StockResult getStock(String sku);

    ReservationResult getReservation(UUID reservationId);
}
