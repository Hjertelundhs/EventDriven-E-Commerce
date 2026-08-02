package com.eventdrivencommerce.product;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiIT {

    private static final String REDIS_PASSWORD = "integration-test-password";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.9-alpine")
            .withDatabaseName("product_db")
            .withUsername("product_app")
            .withPassword("product-test-password");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:8.8.0-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", REDIS_PASSWORD);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.9.1"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("management.tracing.enabled", () -> false);
        registry.add("product.outbox.poll-interval", () -> "PT0.1S");
    }

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM products");
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void persistsSearchesCachesAndPublishesProductChange() {
        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> request = Map.of(
                "sku", "SKU-100",
                "name", "Mechanical Keyboard",
                "description", "Hot-swappable keyboard",
                "category", "Peripherals",
                "price", 1499.00,
                "currency", "SEK"
        );

        String productId = RestAssured.given()
                .header("X-Correlation-ID", correlationId)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/v1/products")
                .then().statusCode(201)
                .header("X-Correlation-ID", correlationId)
                .extract().path("id");

        RestAssured.when().get("/api/v1/products/{id}", productId)
                .then().statusCode(200).body("sku", org.hamcrest.Matchers.equalTo("SKU-100"));
        RestAssured.given().queryParam("name", "keyboard").queryParam("category", "Peripherals")
                .when().get("/api/v1/products")
                .then().statusCode(200).body("totalElements", org.hamcrest.Matchers.equalTo(1));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM products", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1L);

        try (KafkaConsumer<String, String> consumer = consumer()) {
            consumer.subscribe(List.of("commerce.product.v1"));
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                var records = consumer.poll(Duration.ofMillis(500));
                assertThat(records).anySatisfy(record -> {
                    assertThat(record.key()).isEqualTo(productId);
                    assertThat(record.value()).contains("ProductChangedV1", correlationId, "CREATED");
                });
            });
        }
    }

    private static KafkaConsumer<String, String> consumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "product-api-it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ));
    }
}
