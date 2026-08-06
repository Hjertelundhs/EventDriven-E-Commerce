package com.eventdrivencommerce.order.infrastructure.persistence;
import com.eventdrivencommerce.order.domain.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="orders", uniqueConstraints=@UniqueConstraint(name="uk_orders_customer_idempotency",columnNames={"customer_id","idempotency_key"}))
class OrderJpaEntity {
    @Id UUID id;
    @Column(name="customer_id",nullable=false) UUID customerId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) OrderStatus status;
    @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.EAGER) @OrderBy("id") List<OrderLineJpaEntity> lines=new ArrayList<>();
    @Column(name="total_amount",nullable=false,precision=19,scale=2) BigDecimal totalAmount;
    @Column(nullable=false,length=3) String currency;
    @Embedded @AttributeOverrides({@AttributeOverride(name="recipient",column=@Column(name="shipping_recipient",nullable=false)),@AttributeOverride(name="line1",column=@Column(name="shipping_line1",nullable=false)),@AttributeOverride(name="line2",column=@Column(name="shipping_line2")),@AttributeOverride(name="postalCode",column=@Column(name="shipping_postal_code",nullable=false)),@AttributeOverride(name="city",column=@Column(name="shipping_city",nullable=false)),@AttributeOverride(name="countryCode",column=@Column(name="shipping_country_code",nullable=false,length=2))}) AddressEmbeddable shippingAddress;
    @Embedded @AttributeOverrides({@AttributeOverride(name="recipient",column=@Column(name="billing_recipient",nullable=false)),@AttributeOverride(name="line1",column=@Column(name="billing_line1",nullable=false)),@AttributeOverride(name="line2",column=@Column(name="billing_line2")),@AttributeOverride(name="postalCode",column=@Column(name="billing_postal_code",nullable=false)),@AttributeOverride(name="city",column=@Column(name="billing_city",nullable=false)),@AttributeOverride(name="countryCode",column=@Column(name="billing_country_code",nullable=false,length=2))}) AddressEmbeddable billingAddress;
    @Column(name="idempotency_key",nullable=false,length=120) String idempotencyKey;
    @Column(name="request_fingerprint",nullable=false,length=64) String requestFingerprint;
    @Column(name="created_at",nullable=false,updatable=false) Instant createdAt;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    @Version long version;
    @Column(name="inventory_released",nullable=false) boolean inventoryReleased;
    @Column(name="refund_completed",nullable=false) boolean refundCompleted;
    @Column(name="cancellation_reason",length=120) String cancellationReason;
    protected OrderJpaEntity(){}
    static OrderJpaEntity from(Order order){var e=new OrderJpaEntity();e.apply(order);e.lines=new ArrayList<>(order.lines().stream().map(l->OrderLineJpaEntity.from(l,e)).toList());return e;}
    void apply(Order o){id=o.id();customerId=o.customerId();status=o.status();totalAmount=o.totalAmount();currency=o.currency();shippingAddress=AddressEmbeddable.from(o.shippingAddress());billingAddress=AddressEmbeddable.from(o.billingAddress());idempotencyKey=o.idempotencyKey();requestFingerprint=o.requestFingerprint();createdAt=o.createdAt();updatedAt=o.updatedAt();inventoryReleased=o.inventoryReleasedFlag();refundCompleted=o.refundCompletedFlag();cancellationReason=o.cancellationReason();}
    Order toDomain(){return Order.restore(id,customerId,status,lines.stream().map(OrderLineJpaEntity::toDomain).toList(),totalAmount,currency,shippingAddress.toDomain(),billingAddress.toDomain(),idempotencyKey,requestFingerprint,createdAt,updatedAt,version,inventoryReleased,refundCompleted,cancellationReason);}
}
