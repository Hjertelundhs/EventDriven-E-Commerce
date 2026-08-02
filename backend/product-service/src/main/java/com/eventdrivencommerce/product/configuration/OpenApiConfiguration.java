package com.eventdrivencommerce.product.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI productServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Product Service API")
                        .description("Product catalog commands and queries for the Order & Logistics Platform")
                        .version("v1")
                        .contact(new Contact().name("EventDriven-E-Commerce"))
                        .license(new License().name("Portfolio project")))
                .components(new Components());
    }
}
