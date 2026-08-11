package com.eventdrivencommerce.payment.contract;
import org.junit.jupiter.api.Test;import java.nio.file.*;import static org.assertj.core.api.Assertions.assertThat;
class ComposeContractTest{@Test void composeWiresPaymentDatabaseKafkaAndHealth()throws Exception{String compose=Files.readString(Path.of("..","..","docker-compose.yml"));assertThat(compose).contains("payment-service:","PAYMENT_DB_URL:","PAYMENT_SIMULATION_MODE:","KAFKA_BOOTSTRAP_SERVERS: kafka:9092","8085","/actuator/health/readiness");}}
