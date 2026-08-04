package com.eventdrivencommerce.inventory;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryApiIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.9-alpine")
            .withDatabaseName("inventory_db")
            .withUsername("inventory_app")
            .withPassword("inventory-test-password");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:1");
        registry.add("management.tracing.enabled", () -> false);
    }

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM inventory_reservations");
        jdbcTemplate.update("DELETE FROM inventory_items");
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void receivesReservesReleasesAndPreservesStockInvariant() {
        String correlationId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();

        Response receipt = RestAssured.given()
                .header("X-Correlation-ID", correlationId)
                .contentType(ContentType.JSON)
                .body(Map.of("quantity", 10, "version", 0))
                .when().post("/api/v1/inventory/{sku}/receipts", "SKU-100")
                .then().statusCode(200)
                .header("X-Correlation-ID", correlationId)
                .body("availableQuantity", equalTo(10))
                .body("reservedQuantity", equalTo(0))
                .extract().response();

        long receiptVersion = receipt.jsonPath().getLong("version");
        Response reserved = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("orderId", orderId.toString(), "sku", "sku-100", "quantity", 4,
                        "inventoryVersion", receiptVersion))
                .when().post("/api/v1/inventory/reservations")
                .then().statusCode(201)
                .body("status", equalTo("RESERVED"))
                .body("inventoryVersion", equalTo((int) receiptVersion + 1))
                .extract().response();

        String reservationId = reserved.jsonPath().getString("id");
        long reservationVersion = reserved.jsonPath().getLong("version");
        long reservedInventoryVersion = reserved.jsonPath().getLong("inventoryVersion");

        RestAssured.when().get("/api/v1/inventory/{sku}", "SKU-100")
                .then().statusCode(200)
                .body("availableQuantity", equalTo(6))
                .body("reservedQuantity", equalTo(4))
                .body("totalQuantity", equalTo(10));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("reservationVersion", reservationVersion,
                        "inventoryVersion", reservedInventoryVersion))
                .when().post("/api/v1/inventory/reservations/{id}/release", reservationId)
                .then().statusCode(200)
                .body("status", equalTo("RELEASED"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("reservationVersion", 0, "inventoryVersion", 0))
                .when().post("/api/v1/inventory/reservations/{id}/release", reservationId)
                .then().statusCode(200)
                .body("status", equalTo("RELEASED"));

        RestAssured.when().get("/api/v1/inventory/{sku}", "SKU-100")
                .then().statusCode(200)
                .body("availableQuantity", equalTo(10))
                .body("reservedQuantity", equalTo(0))
                .body("totalQuantity", equalTo(10));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM inventory_items", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM inventory_reservations", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM inventory_items WHERE total_quantity <> available_quantity + reserved_quantity",
                Long.class)).isZero();
    }

    @Test
    void returnsConflictForStaleReceiptVersion() {
        long currentVersion = RestAssured.given()
                .contentType(ContentType.JSON).body(Map.of("quantity", 5, "version", 0))
                .when().post("/api/v1/inventory/{sku}/receipts", "SKU-200")
                .then().statusCode(200)
                .extract().jsonPath().getLong("version");

        RestAssured.given().contentType(ContentType.JSON)
                .body(Map.of("quantity", 1, "version", currentVersion + 1))
                .when().post("/api/v1/inventory/{sku}/receipts", "SKU-200")
                .then().statusCode(409)
                .body("type", equalTo(
                        "https://errors.event-driven-commerce.test/inventory/concurrent-modification"));
    }
}
