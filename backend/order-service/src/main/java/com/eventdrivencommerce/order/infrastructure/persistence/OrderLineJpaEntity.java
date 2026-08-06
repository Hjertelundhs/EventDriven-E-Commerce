package com.eventdrivencommerce.order.infrastructure.persistence;
import com.eventdrivencommerce.order.domain.model.OrderLine;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="order_lines") class OrderLineJpaEntity {
    @Id UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id") OrderJpaEntity order;
    @Column(name="product_id",nullable=false) UUID productId;
    @Column(nullable=false,length=80) String sku;
    @Column(name="product_name",nullable=false,length=200) String productName;
    @Column(nullable=false) int quantity;
    @Column(name="unit_price",nullable=false,precision=19,scale=2) BigDecimal unitPrice;
    @Column(name="total_price",nullable=false,precision=19,scale=2) BigDecimal totalPrice;
    protected OrderLineJpaEntity(){}
    static OrderLineJpaEntity from(OrderLine line, OrderJpaEntity order){var e=new OrderLineJpaEntity();e.id=UUID.randomUUID();e.order=order;e.productId=line.productId();e.sku=line.sku();e.productName=line.productName();e.quantity=line.quantity();e.unitPrice=line.unitPrice();e.totalPrice=line.totalPrice();return e;}
    OrderLine toDomain(){return new OrderLine(productId,sku,productName,quantity,unitPrice,totalPrice);}
}
