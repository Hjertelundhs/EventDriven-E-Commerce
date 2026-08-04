package com.eventdrivencommerce.inventory.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryReservationTest {

    private static final Instant NOW = Instant.parse("2026-08-04T12:00:00Z");

    @Test
    void releasesOnlyOnce() {
        InventoryReservation reservation = reservation();

        assertThat(reservation.release(NOW.plusSeconds(1))).isTrue();
        assertThat(reservation.release(NOW.plusSeconds(2))).isFalse();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(reservation.updatedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void completesOnlyOnce() {
        InventoryReservation reservation = reservation();

        assertThat(reservation.complete(NOW.plusSeconds(1))).isTrue();
        assertThat(reservation.complete(NOW.plusSeconds(2))).isFalse();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.COMPLETED);
    }

    @Test
    void rejectsTransitionBetweenTerminalStates() {
        InventoryReservation released = reservation();
        released.release(NOW.plusSeconds(1));

        assertThatThrownBy(() -> released.complete(NOW.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class);

        InventoryReservation completed = reservation();
        completed.complete(NOW.plusSeconds(1));
        assertThatThrownBy(() -> completed.release(NOW.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static InventoryReservation reservation() {
        return InventoryReservation.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new Sku("SKU-100"), 2, NOW);
    }
}
