package com.eventdrivencommerce.inventory.application.service;

import com.eventdrivencommerce.inventory.application.exception.ConcurrentInventoryModificationException;
import com.eventdrivencommerce.inventory.application.exception.DuplicateReservationException;
import com.eventdrivencommerce.inventory.application.exception.InventoryNotFoundException;
import com.eventdrivencommerce.inventory.application.exception.ReservationNotFoundException;
import com.eventdrivencommerce.inventory.application.model.AdjustStockCommand;
import com.eventdrivencommerce.inventory.application.model.ChangeReservationCommand;
import com.eventdrivencommerce.inventory.application.model.ReceiveStockCommand;
import com.eventdrivencommerce.inventory.application.model.ReservationResult;
import com.eventdrivencommerce.inventory.application.model.ReserveStockCommand;
import com.eventdrivencommerce.inventory.application.model.ReserveStockResult;
import com.eventdrivencommerce.inventory.application.model.StockResult;
import com.eventdrivencommerce.inventory.application.port.in.InventoryCommandUseCase;
import com.eventdrivencommerce.inventory.application.port.in.InventoryQueryUseCase;
import com.eventdrivencommerce.inventory.application.port.out.InventoryRepository;
import com.eventdrivencommerce.inventory.application.port.out.ReservationRepository;
import com.eventdrivencommerce.inventory.domain.model.InventoryItem;
import com.eventdrivencommerce.inventory.domain.model.InventoryReservation;
import com.eventdrivencommerce.inventory.domain.model.ReservationStatus;
import com.eventdrivencommerce.inventory.domain.model.Sku;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class InventoryApplicationService implements InventoryCommandUseCase, InventoryQueryUseCase {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public InventoryApplicationService(
            InventoryRepository inventoryRepository,
            ReservationRepository reservationRepository,
            Clock clock) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StockResult receive(ReceiveStockCommand command) {
        Sku sku = new Sku(command.sku());
        Instant now = clock.instant();
        InventoryItem item = inventoryRepository.findBySku(sku).orElseGet(() -> {
            if (command.expectedVersion() != 0) {
                throw concurrent(sku.value());
            }
            return InventoryItem.create(UUID.randomUUID(), sku, now);
        });
        assertVersion(item.version(), command.expectedVersion(), sku.value());
        item.receive(command.quantity(), now);
        return StockResult.from(saveInventory(item));
    }

    @Override
    @Transactional
    public StockResult adjust(AdjustStockCommand command) {
        InventoryItem item = findInventory(new Sku(command.sku()));
        assertVersion(item.version(), command.expectedVersion(), item.sku().value());
        item.adjustTotal(command.totalQuantity(), clock.instant());
        return StockResult.from(saveInventory(item));
    }

    @Override
    @Transactional
    public ReserveStockResult reserve(ReserveStockCommand command) {
        Objects.requireNonNull(command.orderId(), "Order ID is required");
        Sku sku = new Sku(command.sku());
        InventoryReservation existing = reservationRepository.findByOrderIdAndSku(command.orderId(), sku).orElse(null);
        if (existing != null) {
            if (existing.quantity() != command.quantity()) {
                throw new DuplicateReservationException(command.orderId(), sku.value());
            }
            InventoryItem existingItem = findInventory(existing.inventoryItemId(), sku.value());
            return new ReserveStockResult(ReservationResult.from(existing, existingItem.version()), false);
        }

        InventoryItem item = findInventory(sku);
        assertVersion(item.version(), command.expectedInventoryVersion(), sku.value());
        Instant now = clock.instant();
        item.reserve(command.quantity(), now);
        InventoryReservation reservation = InventoryReservation.create(
                UUID.randomUUID(), item.id(), command.orderId(), sku, command.quantity(), now);
        InventoryItem savedItem = saveInventory(item);
        InventoryReservation savedReservation = saveReservation(reservation);
        return new ReserveStockResult(ReservationResult.from(savedReservation, savedItem.version()), true);
    }

    @Override
    @Transactional
    public ReservationResult release(ChangeReservationCommand command) {
        Objects.requireNonNull(command.reservationId(), "Reservation ID is required");
        InventoryReservation reservation = findReservation(command.reservationId());
        InventoryItem item = findInventory(reservation.inventoryItemId(), reservation.sku().value());
        if (reservation.status() == ReservationStatus.RELEASED) {
            return ReservationResult.from(reservation, item.version());
        }
        assertVersion(reservation.version(), command.expectedReservationVersion(), reservation.id().toString());
        assertVersion(item.version(), command.expectedInventoryVersion(), item.sku().value());
        Instant now = clock.instant();
        reservation.release(now);
        item.release(reservation.quantity(), now);
        InventoryItem savedItem = saveInventory(item);
        return ReservationResult.from(saveReservation(reservation), savedItem.version());
    }

    @Override
    @Transactional
    public ReservationResult complete(ChangeReservationCommand command) {
        Objects.requireNonNull(command.reservationId(), "Reservation ID is required");
        InventoryReservation reservation = findReservation(command.reservationId());
        InventoryItem item = findInventory(reservation.inventoryItemId(), reservation.sku().value());
        if (reservation.status() == ReservationStatus.COMPLETED) {
            return ReservationResult.from(reservation, item.version());
        }
        assertVersion(reservation.version(), command.expectedReservationVersion(), reservation.id().toString());
        assertVersion(item.version(), command.expectedInventoryVersion(), item.sku().value());
        Instant now = clock.instant();
        reservation.complete(now);
        item.complete(reservation.quantity(), now);
        InventoryItem savedItem = saveInventory(item);
        return ReservationResult.from(saveReservation(reservation), savedItem.version());
    }

    @Override
    @Transactional(readOnly = true)
    public StockResult getStock(String sku) {
        return StockResult.from(findInventory(new Sku(sku)));
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResult getReservation(UUID reservationId) {
        Objects.requireNonNull(reservationId, "Reservation ID is required");
        InventoryReservation reservation = findReservation(reservationId);
        InventoryItem item = findInventory(reservation.inventoryItemId(), reservation.sku().value());
        return ReservationResult.from(reservation, item.version());
    }

    private InventoryItem findInventory(Sku sku) {
        return inventoryRepository.findBySku(sku).orElseThrow(() -> new InventoryNotFoundException(sku.value()));
    }

    private InventoryItem findInventory(UUID inventoryItemId, String sku) {
        return inventoryRepository.findById(inventoryItemId)
                .orElseThrow(() -> new InventoryNotFoundException(sku));
    }

    private InventoryReservation findReservation(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }

    private InventoryItem saveInventory(InventoryItem item) {
        try {
            return inventoryRepository.save(item);
        } catch (OptimisticLockingFailureException exception) {
            throw concurrent(item.sku().value());
        }
    }

    private InventoryReservation saveReservation(InventoryReservation reservation) {
        try {
            return reservationRepository.save(reservation);
        } catch (OptimisticLockingFailureException exception) {
            throw concurrent(reservation.id().toString());
        }
    }

    private static void assertVersion(long actual, long expected, String resource) {
        if (actual != expected) {
            throw concurrent(resource);
        }
    }

    private static ConcurrentInventoryModificationException concurrent(String resource) {
        return new ConcurrentInventoryModificationException(resource);
    }
}
