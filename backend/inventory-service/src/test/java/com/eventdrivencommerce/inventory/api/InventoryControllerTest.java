package com.eventdrivencommerce.inventory.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.eventdrivencommerce.inventory.api.dto.ReceiveStockRequest;
import com.eventdrivencommerce.inventory.api.dto.ReserveStockRequest;
import com.eventdrivencommerce.inventory.application.model.ReservationResult;
import com.eventdrivencommerce.inventory.application.model.ReserveStockResult;
import com.eventdrivencommerce.inventory.application.model.StockResult;
import com.eventdrivencommerce.inventory.application.port.in.InventoryCommandUseCase;
import com.eventdrivencommerce.inventory.application.port.in.InventoryQueryUseCase;
import com.eventdrivencommerce.inventory.domain.model.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventoryControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-04T12:00:00Z");

    private InventoryCommandUseCase commands;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        commands = mock(InventoryCommandUseCase.class);
        InventoryQueryUseCase queries = mock(InventoryQueryUseCase.class);
        InventoryController controller = new InventoryController(commands, queries, new InventoryApiMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new CorrelationIdFilter())
                .build();
        objectMapper = JsonMapper.builder().findAndAddModules().build();
    }

    @Test
    void returnsReceivedStockWithEtagAndCorrelationId() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(commands.receive(any())).thenReturn(new StockResult(itemId, "SKU-100", 10, 0, 10, NOW, NOW, 1));

        mockMvc.perform(post("/api/v1/inventory/SKU-100/receipts")
                        .header(CorrelationIdFilter.CORRELATION_HEADER,
                                "018f5f58-e584-7b04-b9ac-2e5c7d6e9f70")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReceiveStockRequest(10, 0))))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_HEADER,
                        "018f5f58-e584-7b04-b9ac-2e5c7d6e9f70"))
                .andExpect(jsonPath("$.sku").value("SKU-100"))
                .andExpect(jsonPath("$.availableQuantity").value(10));
    }

    @Test
    void returnsCreatedReservationWithLocation() throws Exception {
        UUID reservationId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        ReservationResult reservation = new ReservationResult(reservationId, orderId, "SKU-100", 2,
                ReservationStatus.RESERVED, NOW, NOW, 0, 2);
        when(commands.reserve(any())).thenReturn(new ReserveStockResult(reservation, true));

        mockMvc.perform(post("/api/v1/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ReserveStockRequest(orderId, "SKU-100", 2, 1))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "http://localhost/api/v1/inventory/reservations/" + reservationId))
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.id").value(reservationId.toString()))
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void returnsProblemDetailsForInvalidReservation() throws Exception {
        ReserveStockRequest request = new ReserveStockRequest(null, "x", 0, -1);

        mockMvc.perform(post("/api/v1/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(
                        "https://errors.event-driven-commerce.test/inventory/request-validation"))
                .andExpect(jsonPath("$.violations").isArray());
    }
}
