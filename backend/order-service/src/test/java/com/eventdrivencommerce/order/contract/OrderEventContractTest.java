package com.eventdrivencommerce.order.contract;
import com.fasterxml.jackson.databind.*;import org.junit.jupiter.api.Test;import java.nio.file.*;import static org.assertj.core.api.Assertions.assertThat;
class OrderEventContractTest {
 private final ObjectMapper mapper=new ObjectMapper();
 @Test void schemasAndExamplesExposeStableV1Envelope()throws Exception{for(String name:new String[]{"order-created-v1","order-completed-v1","order-cancelled-v1"}){Path root=Path.of("..","shared-contracts","events");JsonNode schema=mapper.readTree(Files.readString(root.resolve(name+".schema.json")));JsonNode example=mapper.readTree(Files.readString(root.resolve("examples").resolve(name+".json")));assertThat(schema.path("$schema").asText()).isEqualTo("https://json-schema.org/draft/2020-12/schema");assertThat(example.path("eventVersion").asInt()).isEqualTo(1);assertThat(example.path("eventType").asText()).isEqualTo(schema.path("properties").path("eventType").path("const").asText());assertThat(example.path("payload").path("orderId").isTextual()).isTrue();}}
}
