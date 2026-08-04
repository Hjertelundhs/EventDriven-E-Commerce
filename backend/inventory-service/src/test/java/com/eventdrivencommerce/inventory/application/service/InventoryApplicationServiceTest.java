package com.eventdrivencommerce.inventory.application.service;

import com.eventdrivencommerce.inventory.application.exception.ConcurrentInventoryModificationException;
import com.eventdrivencommerce.inventory.application.exception.DuplicateReservationException;
import com.eventdrivencommerce.inventory.application.model.ChangeReservationCommand;
import com.eventdrivencommerce.inventory.application.model.ReceiveStockCommand;
import com.eventdrivencommerce.inventory.application.model.ReserveStockCommand;
import com.eventdrivencommerce.inventory.application.port.out.InventoryRepository;
import com.eventdrivencommerce.inventory.application.port.out.ReservationRepository;
import com.eventdrivencommerce.inventory.domain.model.InventoryItem;
import com.eventdrivencommerce.inventory.domain.model.InventoryReservation;
import com.eventdrivencommerce.inventory.domain.model.ReservationStatus;
import com.eventdrivencommerce.inventory.domain.model.Sku;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T12:00:00Z");
    private static final Sku SKU = new Sku("SKU-100");

    @Mock InventoryRepository inventoryRepository;
    @Mock ReservationRepository reservationRepository;

    private InventoryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new InventoryApplicationService(inventoryRepository, reservationRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsInventoryOnFirstReceipt() {
        when(inventoryRepository.findBySku(SKU)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.receive(new ReceiveStockCommand("sku-100", 10, 0));

        assertThat(result.sku()).isEqualTo("SKU-100");
        assertThat(result.availableQuantity()).isEqualTo(10);
        assertThat(result.totalQuantity()).isEqualTo(10);
    }

    @Test
    void reservesAvailableStockAndCreatesReservation() {
        UUID orderId = UUID.randomUUID();
        InventoryItem item = item(10, 0, 10, 2);
        when(reservationRepository.findByOrderIdAndSku(orderId, SKU)).thenReturn(Optional.empty());
        when(inventoryRepository.findBySku(SKU)).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.reserve(new ReserveStockCommand(orderId, "SKU-100", 4, 2));

        assertThat(result.created()).isTrue();
        assertThat(result.reservation().status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(item.availableQuantity()).isEqualTo(6);
        assertThat(item.reservedQuantity()).isEqualTo(4);
    }

    @Test
    void returnsExistingReservationForRepeatedIdenticalRequest() {
        UUID orderId = UUID.randomUUID();
        InventoryItem item = item(6, 4, 10, 3);
        InventoryReservation reservation = InventoryReservation.rehydrate(UUID.randomUUID(), item.id(), orderId, SKU,
                4, ReservationStatus.RESERVED, NOW.minusSeconds(10), NOW.minusSeconds(10), 0);
        when(reservationRepository.findByOrderIdAndSku(orderId, SKU)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findById(item.id())).thenReturn(Optional.of(item));

        var result = service.reserve(new ReserveStockCommand(orderId, "SKU-100", 4, 0));

        assertThat(result.created()).isFalse();
        assertThat(result.reservation().id()).isEqualTo(reservation.id());
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void rejectsRepeatedReservationWithDifferentQuantity() {
        UUID orderId = UUID.randomUUID();
        InventoryItem item = item(6, 4, 10, 3);
        InventoryReservation reservation = InventoryReservation.create(UUID.randomUUID(), item.id(), orderId, SKU,
                4, NOW.minusSeconds(1));
        when(reservationRepository.findByOrderIdAndSku(orderId, SKU)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.reserve(new ReserveStockCommand(orderId, "SKU-100", 5, 3)))
                .isInstanceOf(DuplicateReservationException.class);
    }

    @Test
    void releasesStockAndTreatsRepeatAsIdempotent() {
        UUID orderId = UUID.randomUUID();
        InventoryItem item = item(6, 4, 10, 3);
        InventoryReservation reservation = InventoryReservation.rehydrate(UUID.randomUUID(), item.id(), orderId, SKU,
                4, ReservationStatus.RESERVED, NOW.minusSeconds(10), NOW.minusSeconds(10), 1);
        when(reservationRepository.findById(reservation.id())).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findById(item.id())).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var released = service.release(new ChangeReservationCommand(reservation.id(), 1, 3));
        var repeated = service.release(new ChangeReservationCommand(reservation.id(), 0, 0));

        assertThat(released.status()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(repeated.status()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(item.availableQuantity()).isEqualTo(10);
        assertThat(item.reservedQuantity()).isZero();
        verify(inventoryRepository).save(item);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void rejectsStaleInventoryVersionBeforeMutation() {
        InventoryItem item = item(10, 0, 10, 4);
        when(inventoryRepository.findBySku(SKU)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.receive(new ReceiveStockCommand("SKU-100", 1, 3)))
                .isInstanceOf(ConcurrentInventoryModificationException.class);

        assertThat(item.totalQuantity()).isEqualTo(10);
        verify(inventoryRepository, never()).save(any());
    }

    private static InventoryItem item(int available, int reserved, int total, long version) {
        return InventoryItem.rehydrate(UUID.randomUUID(), SKU, available, reserved, total,
                NOW.minusSeconds(60), NOW.minusSeconds(30), version);
    }
}
