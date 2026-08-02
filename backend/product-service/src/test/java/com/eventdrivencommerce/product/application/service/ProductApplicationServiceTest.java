package com.eventdrivencommerce.product.application.service;

import com.eventdrivencommerce.product.application.event.ProductChangedV1;
import com.eventdrivencommerce.product.application.exception.ConcurrentProductModificationException;
import com.eventdrivencommerce.product.application.exception.DuplicateSkuException;
import com.eventdrivencommerce.product.application.model.CreateProductCommand;
import com.eventdrivencommerce.product.application.model.UpdateProductCommand;
import com.eventdrivencommerce.product.application.port.out.ProductEventOutbox;
import com.eventdrivencommerce.product.application.port.out.ProductRepository;
import com.eventdrivencommerce.product.domain.model.Money;
import com.eventdrivencommerce.product.domain.model.Product;
import com.eventdrivencommerce.product.domain.model.Sku;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");

    @Mock ProductRepository repository;
    @Mock ProductEventOutbox outbox;

    private ProductApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ProductApplicationService(repository, outbox, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsProductAndAppendsEventAfterPersistence() {
        UUID correlationId = UUID.randomUUID();
        UUID causationId = UUID.randomUUID();
        when(repository.existsBySku(new Sku("SKU-100"))).thenReturn(false);
        when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(new CreateProductCommand("sku-100", "Keyboard", "Mechanical", "Peripherals",
                new BigDecimal("1499.00"), "SEK", correlationId, causationId));

        assertThat(result.sku()).isEqualTo("SKU-100");
        ArgumentCaptor<ProductChangedV1> event = ArgumentCaptor.forClass(ProductChangedV1.class);
        verify(outbox).append(event.capture());
        assertThat(event.getValue().aggregateId()).isEqualTo(result.id());
        assertThat(event.getValue().payload().changeType()).isEqualTo(ProductChangedV1.ChangeType.CREATED);
        assertThat(event.getValue().correlationId()).isEqualTo(correlationId);
    }

    @Test
    void rejectsDuplicateSkuBeforePersistence() {
        when(repository.existsBySku(new Sku("SKU-100"))).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateProductCommand("SKU-100", "Keyboard", "", "Peripherals",
                new BigDecimal("10.00"), "SEK", UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(DuplicateSkuException.class);

        verify(repository, never()).save(any());
        verify(outbox, never()).append(any());
    }

    @Test
    void rejectsStaleVersionBeforeUpdating() {
        UUID productId = UUID.randomUUID();
        Product existing = Product.rehydrate(productId, new Sku("SKU-100"), "Keyboard", "", "Peripherals",
                Money.of(new BigDecimal("10.00"), "SEK"), true, NOW.minusSeconds(30), NOW.minusSeconds(30), 4);
        when(repository.findById(productId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(new UpdateProductCommand(productId, "SKU-100", "Keyboard", "",
                "Peripherals", new BigDecimal("12.00"), "SEK", 3, UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(ConcurrentProductModificationException.class);

        verify(repository, never()).save(any());
        verify(outbox, never()).append(any());
    }
}
