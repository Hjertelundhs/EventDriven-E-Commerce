package com.eventdrivencommerce.order.configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration public class OpenApiConfiguration { @Bean OpenAPI orderOpenApi(){return new OpenAPI().info(new Info().title("Order Service API").version("v1").description("Customer-scoped order registration and saga status API"));} }
