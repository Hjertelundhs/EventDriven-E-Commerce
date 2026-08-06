package com.eventdrivencommerce.order.domain.model;

import com.eventdrivencommerce.order.domain.exception.InvalidOrderTransitionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Order {
    private final UUID id; private final UUID customerId; private OrderStatus status;
    private final List<OrderLine> lines; private final BigDecimal totalAmount; private final String currency;
    private final Address shippingAddress; private final Address billingAddress; private final String idempotencyKey;
    private final String requestFingerprint; private final Instant createdAt; private Instant updatedAt; private long version;
    private boolean inventoryReleased; private boolean refundCompleted; private String cancellationReason;

    private Order(UUID id, UUID customerId, OrderStatus status, List<OrderLine> lines, BigDecimal totalAmount,
                  String currency, Address shippingAddress, Address billingAddress, String idempotencyKey,
                  String requestFingerprint, Instant createdAt, Instant updatedAt, long version,
                  boolean inventoryReleased, boolean refundCompleted, String cancellationReason) {
        this.id = id; this.customerId = customerId; this.status = status; this.lines = List.copyOf(lines);
        this.totalAmount = OrderLine.money(totalAmount); this.currency = currency.toUpperCase();
        this.shippingAddress = shippingAddress; this.billingAddress = billingAddress; this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint; this.createdAt = createdAt; this.updatedAt = updatedAt; this.version = version;
        this.inventoryReleased = inventoryReleased; this.refundCompleted = refundCompleted; this.cancellationReason = cancellationReason;
    }

    public static Order create(UUID customerId, List<OrderLine> lines, String currency, Address shipping, Address billing,
                               String idempotencyKey, String fingerprint, Instant now) {
        if (customerId == null || lines == null || lines.isEmpty()) throw new IllegalArgumentException("customer and lines are required");
        if (currency == null || currency.length() != 3) throw new IllegalArgumentException("currency must be ISO 4217");
        BigDecimal total = lines.stream().map(OrderLine::totalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Order(UUID.randomUUID(), customerId, OrderStatus.PENDING, lines, total, currency, shipping, billing,
                idempotencyKey, fingerprint, now, now, 0, false, false, null);
    }

    public static Order restore(UUID id, UUID customerId, OrderStatus status, List<OrderLine> lines, BigDecimal total,
                                String currency, Address shipping, Address billing, String key, String fingerprint,
                                Instant created, Instant updated, long version, boolean inventoryReleased,
                                boolean refundCompleted, String reason) {
        return new Order(id, customerId, status, lines, total, currency, shipping, billing, key, fingerprint,
                created, updated, version, inventoryReleased, refundCompleted, reason);
    }

    public void inventoryReserved(Instant now) { require(OrderStatus.PENDING, "InventoryReservedV1"); status = OrderStatus.INVENTORY_RESERVED; touch(now); status = OrderStatus.PAYMENT_PENDING; }
    public void inventoryReservationFailed(String reason, Instant now) { require(OrderStatus.PENDING, "InventoryReservationFailedV1"); cancel(reason, now); }
    public void paymentCompleted(Instant now) { require(OrderStatus.PAYMENT_PENDING, "PaymentCompletedV1"); status = OrderStatus.PAID; touch(now); }
    public void paymentFailed(Instant now) { require(OrderStatus.PAYMENT_PENDING, "PaymentFailedV1"); status = OrderStatus.PAYMENT_FAILED; touch(now); }
    public void deliveryCreated(Instant now) { require(OrderStatus.PAID, "DeliveryCreatedV1"); status = OrderStatus.DELIVERY_CREATED; touch(now); }
    public void delivered(Instant now) { require(OrderStatus.DELIVERY_CREATED, "DeliveryStatusChangedV1(DELIVERED)"); status = OrderStatus.COMPLETED; touch(now); }
    public void deliveryFailed(Instant now) { if (status != OrderStatus.PAID && status != OrderStatus.DELIVERY_CREATED) throw new InvalidOrderTransitionException(status, "DeliveryFailedV1"); status = OrderStatus.DELIVERY_FAILED; touch(now); }
    public void inventoryReleased(Instant now) { if (status != OrderStatus.PAYMENT_FAILED && status != OrderStatus.DELIVERY_FAILED) throw new InvalidOrderTransitionException(status, "InventoryReleasedV1"); inventoryReleased = true; if (status == OrderStatus.PAYMENT_FAILED || refundCompleted) cancel("DOWNSTREAM_FAILURE", now); else touch(now); }
    public void refundCompleted(Instant now) { require(OrderStatus.DELIVERY_FAILED, "RefundCompletedV1"); refundCompleted = true; if (inventoryReleased) cancel("DELIVERY_FAILED", now); else touch(now); }
    private void cancel(String reason, Instant now) { status = OrderStatus.CANCELLED; cancellationReason = reason == null ? "UNSPECIFIED" : reason; touch(now); }
    private void require(OrderStatus expected, String event) { if (status != expected) throw new InvalidOrderTransitionException(status, event); }
    private void touch(Instant now) { updatedAt = now; }

    public UUID id(){return id;} public UUID customerId(){return customerId;} public OrderStatus status(){return status;}
    public List<OrderLine> lines(){return lines;} public BigDecimal totalAmount(){return totalAmount;} public String currency(){return currency;}
    public Address shippingAddress(){return shippingAddress;} public Address billingAddress(){return billingAddress;}
    public String idempotencyKey(){return idempotencyKey;} public String requestFingerprint(){return requestFingerprint;}
    public Instant createdAt(){return createdAt;} public Instant updatedAt(){return updatedAt;} public long version(){return version;}
    public boolean inventoryReleasedFlag(){return inventoryReleased;} public boolean refundCompletedFlag(){return refundCompleted;}
    public String cancellationReason(){return cancellationReason;}
}
