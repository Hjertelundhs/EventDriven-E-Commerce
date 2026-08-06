package com.eventdrivencommerce.order.api.dto;
import com.eventdrivencommerce.order.domain.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record OrderResponse(UUID id,UUID customerId,OrderStatus status,List<Line> orderLines,BigDecimal totalAmount,String currency,Address shippingAddress,Address billingAddress,Instant createdAt,Instant updatedAt,long version){
    public static OrderResponse from(Order o){return new OrderResponse(o.id(),o.customerId(),o.status(),o.lines().stream().map(Line::from).toList(),o.totalAmount(),o.currency(),o.shippingAddress(),o.billingAddress(),o.createdAt(),o.updatedAt(),o.version());}
    public record Line(UUID productId,String sku,String productName,int quantity,BigDecimal unitPrice,BigDecimal totalPrice){static Line from(OrderLine l){return new Line(l.productId(),l.sku(),l.productName(),l.quantity(),l.unitPrice(),l.totalPrice());}}
}
