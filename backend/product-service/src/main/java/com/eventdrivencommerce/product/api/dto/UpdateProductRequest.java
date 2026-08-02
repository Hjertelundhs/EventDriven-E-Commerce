package com.eventdrivencommerce.product.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{2,63}") String sku,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 4000) String description,
        @NotBlank @Size(max = 120) String category,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal price,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @PositiveOrZero long version
) {}
