package com.eventdrivencommerce.product.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.eventdrivencommerce.product.api.dto.CreateProductRequest;
import com.eventdrivencommerce.product.application.model.ProductResult;
import com.eventdrivencommerce.product.application.port.in.ProductCommandUseCase;
import com.eventdrivencommerce.product.application.port.in.ProductQueryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private ProductCommandUseCase commands;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        commands = mock(ProductCommandUseCase.class);
        ProductQueryUseCase queries = mock(ProductQueryUseCase.class);
        ProductController controller = new ProductController(commands, queries,
                Mappers.getMapper(ProductApiMapper.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new CorrelationIdFilter())
                .build();
        objectMapper = JsonMapper.builder().findAndAddModules().build();
    }

    @Test
    void returnsCreatedProductWithLocationEtagAndCorrelationId() throws Exception {
        UUID id = UUID.randomUUID();
        when(commands.create(any())).thenReturn(new ProductResult(id, "SKU-100", "Keyboard", "Mechanical",
                "Peripherals", new BigDecimal("1499.00"), "SEK", true,
                Instant.parse("2026-08-02T12:00:00Z"), Instant.parse("2026-08-02T12:00:00Z"), 0));
        CreateProductRequest request = new CreateProductRequest("SKU-100", "Keyboard", "Mechanical",
                "Peripherals", new BigDecimal("1499.00"), "SEK");

        mockMvc.perform(post("/api/v1/products")
                        .header(CorrelationIdFilter.CORRELATION_HEADER, "018f5f58-e584-7b04-b9ac-2e5c7d6e9f70")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/products/" + id))
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_HEADER,
                        "018f5f58-e584-7b04-b9ac-2e5c7d6e9f70"))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.sku").value("SKU-100"));
    }

    @Test
    void returnsProblemDetailsForInvalidInput() throws Exception {
        CreateProductRequest request = new CreateProductRequest("x", "", "", "",
                new BigDecimal("-1.00"), "XX");

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(
                        "https://errors.event-driven-commerce.test/product/request-validation"))
                .andExpect(jsonPath("$.violations").isArray());
    }
}
