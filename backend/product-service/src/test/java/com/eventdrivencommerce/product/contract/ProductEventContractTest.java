package com.eventdrivencommerce.product.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eventdrivencommerce.product.application.event.ProductChangedV1;
import com.eventdrivencommerce.product.domain.model.Money;
import com.eventdrivencommerce.product.domain.model.Product;
import com.eventdrivencommerce.product.domain.model.Sku;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void serializedEventMatchesVersionedEnvelopeAndSchemaFields() throws Exception {
        Instant now = Instant.parse("2026-08-02T12:00:00Z");
        Product product = Product.create(UUID.randomUUID(), new Sku("SKU-100"), "Keyboard", "Mechanical",
                "Peripherals", Money.of(new BigDecimal("1499.00"), "SEK"), now);
        ProductChangedV1 event = ProductChangedV1.from(product, ProductChangedV1.ChangeType.CREATED,
                UUID.randomUUID(), UUID.randomUUID(), now);

        JsonNode serialized = objectMapper.valueToTree(event);
        Set<String> actualFields = new HashSet<>();
        serialized.fieldNames().forEachRemaining(actualFields::add);
        assertThat(actualFields).containsExactlyInAnyOrderElementsOf(Set.of(
                "eventId", "eventType", "eventVersion", "aggregateId", "correlationId", "causationId",
                "occurredAt", "payload"));
        assertThat(serialized.path("eventType").asText()).isEqualTo("ProductChangedV1");
        assertThat(serialized.path("payload").path("changeType").asText()).isEqualTo("CREATED");

        Path schema = Path.of("..", "shared-contracts", "events", "product-changed-v1.schema.json");
        JsonNode schemaNode = objectMapper.readTree(Files.readString(schema));
        assertThat(schemaNode.path("$schema").asText()).isEqualTo("https://json-schema.org/draft/2020-12/schema");
        assertThat(schemaNode.path("properties").path("eventVersion").path("const").asInt()).isEqualTo(1);
    }
}
