package com.eventdrivencommerce.product.application.model;

import java.util.UUID;

public record DeactivateProductCommand(
        UUID productId,
        long expectedVersion,
        UUID correlationId,
        UUID causationId
) {}
