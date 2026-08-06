package com.eventdrivencommerce.order.contract;
import org.junit.jupiter.api.Test;import java.nio.file.*;import static org.assertj.core.api.Assertions.assertThat;
class ComposeContractTest {@Test void composeWiresOrderDatabaseProductKafkaAndHealth()throws Exception{String compose=Files.readString(Path.of("..","..","docker-compose.yml"));assertThat(compose).contains("order-service:","ORDER_DB_URL:","PRODUCT_SERVICE_BASE_URL: http://product-service:8082","KAFKA_BOOTSTRAP_SERVERS: kafka:9092","/actuator/health/readiness");}}
