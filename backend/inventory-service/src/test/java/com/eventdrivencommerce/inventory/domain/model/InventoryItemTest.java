package com.eventdrivencommerce.inventory.domain.model;

import com.eventdrivencommerce.inventory.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-03T08:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-08-03T09:00:00Z");

    @Test
    void receivesStockAndMaintainsQuantityBalance() {
        InventoryItem item = newItem();

        item.receive(12, UPDATED_AT);

        assertThat(item.availableQuantity()).isEqualTo(12);
        assertThat(item.reservedQuantity()).isZero();
        assertThat(item.totalQuantity()).isEqualTo(12);
        assertThat(item.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void reservesAvailableStock() {
        InventoryItem item = stockedItem(10);

        item.reserve(4, UPDATED_AT);

        assertThat(item.availableQuantity()).isEqualTo(6);
        assertThat(item.reservedQuantity()).isEqualTo(4);
        assertThat(item.totalQuantity()).isEqualTo(10);
    }

    @Test
    void rejectsReservationThatWouldMakeAvailableStockNegative() {
        InventoryItem item = stockedItem(3);

        assertThatThrownBy(() -> item.reserve(4, UPDATED_AT))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("SKU-100")
                .hasMessageContaining("available 3");

        assertThat(item.availableQuantity()).isEqualTo(3);
        assertThat(item.reservedQuantity()).isZero();
    }

    @Test
    void releasesReservedStockWithoutChangingTotal() {
        InventoryItem item = stockedItem(10);
        item.reserve(4, UPDATED_AT);

        item.release(3, UPDATED_AT.plusSeconds(60));

        assertThat(item.availableQuantity()).isEqualTo(9);
        assertThat(item.reservedQuantity()).isEqualTo(1);
        assertThat(item.totalQuantity()).isEqualTo(10);
    }

    @Test
    void completingReservationRemovesStockFromReservedAndTotal() {
        InventoryItem item = stockedItem(10);
        item.reserve(4, UPDATED_AT);

        item.complete(4, UPDATED_AT.plusSeconds(60));

        assertThat(item.availableQuantity()).isEqualTo(6);
        assertThat(item.reservedQuantity()).isZero();
        assertThat(item.totalQuantity()).isEqualTo(6);
    }

    @Test
    void adjustmentCannotReduceTotalBelowReservedStock() {
        InventoryItem item = stockedItem(10);
        item.reserve(6, UPDATED_AT);

        assertThatThrownBy(() -> item.adjustTotal(5, UPDATED_AT.plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total quantity cannot be lower than reserved quantity");

        assertThat(item.totalQuantity()).isEqualTo(10);
    }

    @Test
    void rehydrationRejectsAnInvalidQuantityBalance() {
        assertThatThrownBy(() -> InventoryItem.rehydrate(
                UUID.randomUUID(), new Sku("SKU-100"), 5, 4, 8, CREATED_AT, UPDATED_AT, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total quantity must equal available plus reserved quantity");
    }

    @Test
    void skuIsNormalizedWithinTheInventoryBoundedContext() {
        assertThat(new Sku(" sku-100 ").value()).isEqualTo("SKU-100");
    }

    private static InventoryItem newItem() {
        return InventoryItem.create(UUID.randomUUID(), new Sku("SKU-100"), CREATED_AT);
    }

    private static InventoryItem stockedItem(int quantity) {
        InventoryItem item = newItem();
        item.receive(quantity, CREATED_AT.plusSeconds(60));
        return item;
    }
}
