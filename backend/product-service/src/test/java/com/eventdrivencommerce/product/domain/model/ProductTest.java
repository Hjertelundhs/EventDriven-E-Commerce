package com.eventdrivencommerce.product.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-02T12:00:00Z");

    @Test
    void createsActiveProductAndNormalizesDomainValues() {
        Product product = Product.create(UUID.randomUUID(), new Sku(" sku-100 "), "  Keyboard ",
                "  Mechanical keyboard  ", " Peripherals ", Money.of(new BigDecimal("1499.00"), "sek"), CREATED_AT);

        assertThat(product.sku().value()).isEqualTo("SKU-100");
        assertThat(product.name()).isEqualTo("Keyboard");
        assertThat(product.description()).isEqualTo("Mechanical keyboard");
        assertThat(product.price().currency().getCurrencyCode()).isEqualTo("SEK");
        assertThat(product.active()).isTrue();
        assertThat(product.version()).isZero();
    }

    @Test
    void preventsUpdatesAfterDeactivation() {
        Product product = product();
        product.deactivate(CREATED_AT.plusSeconds(1));

        assertThatThrownBy(() -> product.update(new Sku("SKU-101"), "New", "Description", "Category",
                Money.of(new BigDecimal("1.00"), "SEK"), CREATED_AT.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void deactivationIsIdempotent() {
        Product product = product();

        assertThat(product.deactivate(CREATED_AT.plusSeconds(1))).isTrue();
        assertThat(product.deactivate(CREATED_AT.plusSeconds(2))).isFalse();
        assertThat(product.updatedAt()).isEqualTo(CREATED_AT.plusSeconds(1));
    }

    @Test
    void rejectsNegativePricesAndUnsupportedPrecision() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-0.01"), "SEK"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of(new BigDecimal("1.001"), "SEK"))
                .isInstanceOf(ArithmeticException.class);
    }

    private static Product product() {
        return Product.create(UUID.randomUUID(), new Sku("SKU-100"), "Keyboard", "Description", "Peripherals",
                Money.of(new BigDecimal("1499.00"), "SEK"), CREATED_AT);
    }
}
