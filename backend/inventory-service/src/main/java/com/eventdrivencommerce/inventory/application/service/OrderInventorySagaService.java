package com.eventdrivencommerce.inventory.application.service;

import com.eventdrivencommerce.inventory.application.model.OrderInventoryRequest;
import com.eventdrivencommerce.inventory.application.port.out.InventoryEventOutbox;
import com.eventdrivencommerce.inventory.application.port.out.InventoryRepository;
import com.eventdrivencommerce.inventory.application.port.out.ReservationRepository;
import com.eventdrivencommerce.inventory.domain.model.InventoryItem;
import com.eventdrivencommerce.inventory.domain.model.InventoryReservation;
import com.eventdrivencommerce.inventory.domain.model.Sku;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderInventorySagaService {
    private final InventoryRepository inventory;
    private final ReservationRepository reservations;
    private final InventoryEventOutbox outbox;
    private final Clock clock;

    public OrderInventorySagaService(InventoryRepository inventory, ReservationRepository reservations,
                                     InventoryEventOutbox outbox, Clock clock) {
        this.inventory = inventory; this.reservations = reservations; this.outbox = outbox; this.clock = clock;
    }

    @Transactional
    public void reserve(OrderInventoryRequest request) {
        Map<Sku, Integer> requested = aggregate(request.lines());
        Map<Sku, InventoryItem> items = new LinkedHashMap<>();
        List<InventoryEventOutbox.Failure> failures = requested.entrySet().stream().map(entry -> {
            InventoryItem item = inventory.findBySku(entry.getKey()).orElse(null);
            if (item == null) return new InventoryEventOutbox.Failure(entry.getKey().value(), entry.getValue(), 0, "SKU_NOT_FOUND");
            items.put(entry.getKey(), item);
            if (item.availableQuantity() < entry.getValue()) return new InventoryEventOutbox.Failure(entry.getKey().value(), entry.getValue(), item.availableQuantity(), "INSUFFICIENT_STOCK");
            return null;
        }).filter(java.util.Objects::nonNull).toList();

        var now = clock.instant();
        if (!failures.isEmpty()) {
            outbox.reservationFailed(request.orderId(), failures, request.correlationId(), request.eventId(), now);
            return;
        }

        for (var entry : requested.entrySet()) {
            Sku sku = entry.getKey(); int quantity = entry.getValue();
            InventoryReservation existing = reservations.findByOrderIdAndSku(request.orderId(), sku).orElse(null);
            if (existing != null) {
                if (existing.quantity() != quantity) throw new IllegalStateException("Existing reservation does not match order quantity for " + sku.value());
                continue;
            }
            InventoryItem item = items.get(sku);
            item.reserve(quantity, now);
            InventoryItem saved = inventory.save(item);
            reservations.save(InventoryReservation.create(UUID.randomUUID(), saved.id(), request.orderId(), sku, quantity, now));
        }
        List<InventoryEventOutbox.Item> eventItems = requested.entrySet().stream()
                .map(entry -> new InventoryEventOutbox.Item(entry.getKey().value(), entry.getValue())).toList();
        outbox.reserved(request.orderId(), eventItems, request.correlationId(), request.eventId(), now);
    }

    private static Map<Sku, Integer> aggregate(List<OrderInventoryRequest.Line> lines) {
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("Order must contain inventory lines");
        Map<Sku, Integer> result = new LinkedHashMap<>();
        for (var line : lines) {
            if (line.quantity() < 1) throw new IllegalArgumentException("Order line quantity must be positive");
            result.merge(new Sku(line.sku()), line.quantity(), Math::addExact);
        }
        return result;
    }
}
