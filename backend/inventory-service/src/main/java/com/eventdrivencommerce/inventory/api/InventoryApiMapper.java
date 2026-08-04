package com.eventdrivencommerce.inventory.api;

import com.eventdrivencommerce.inventory.api.dto.InventoryResponse;
import com.eventdrivencommerce.inventory.api.dto.ReservationResponse;
import com.eventdrivencommerce.inventory.application.model.ReservationResult;
import com.eventdrivencommerce.inventory.application.model.StockResult;
import org.springframework.stereotype.Component;

@Component
public class InventoryApiMapper {

    public InventoryResponse toResponse(StockResult result) {
        return new InventoryResponse(result.id(), result.sku(), result.availableQuantity(), result.reservedQuantity(),
                result.totalQuantity(), result.createdAt(), result.updatedAt(), result.version());
    }

    public ReservationResponse toResponse(ReservationResult result) {
        return new ReservationResponse(result.id(), result.orderId(), result.sku(), result.quantity(), result.status(),
                result.createdAt(), result.updatedAt(), result.version(), result.inventoryVersion());
    }
}
