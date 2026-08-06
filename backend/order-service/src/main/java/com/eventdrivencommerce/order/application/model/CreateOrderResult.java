package com.eventdrivencommerce.order.application.model;

import com.eventdrivencommerce.order.domain.model.Order;
public record CreateOrderResult(Order order, boolean replayed) {}
