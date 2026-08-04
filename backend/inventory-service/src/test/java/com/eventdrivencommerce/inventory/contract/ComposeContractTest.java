package com.eventdrivencommerce.inventory.contract;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComposeContractTest {

    @Test
    void rootComposeContainsIndependentInventoryService() throws Exception {
        Path composeFile = Path.of("..", "..", "docker-compose.yml");
        Map<String, Object> compose = new Yaml(new SafeConstructor(new LoaderOptions()))
                .load(Files.readString(composeFile));

        Map<String, Object> services = map(compose.get("services"));
        Map<String, Object> inventoryService = map(services.get("inventory-service"));
        assertThat(list(inventoryService.get("profiles"))).contains("apps");
        assertThat(map(inventoryService.get("environment")))
                .containsEntry("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
                .containsEntry("INVENTORY_SERVICE_PORT", 8083)
                .doesNotContainKey("PRODUCT_DB_URL");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return (List<Object>) value;
    }
}
