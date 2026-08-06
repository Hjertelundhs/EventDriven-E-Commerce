package com.eventdrivencommerce.order.infrastructure.product;

import com.eventdrivencommerce.order.application.exception.ProductValidationException;
import com.eventdrivencommerce.order.application.model.ProductSnapshot;
import com.eventdrivencommerce.order.application.port.out.ProductCatalog;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class ProductCatalogHttpAdapter implements ProductCatalog {
    private final RestClient client;
    public ProductCatalogHttpAdapter(RestClient.Builder builder,
            @Value("${order.product-service-base-url}") String baseUrl,
            @Value("${order.product-connect-timeout:PT2S}") Duration connectTimeout,
            @Value("${order.product-read-timeout:PT3S}") Duration readTimeout) {
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(connectTimeout);factory.setReadTimeout(readTimeout);
        this.client=builder.baseUrl(baseUrl).requestFactory(factory).build();
    }
    @Override @Retry(name="productCatalog") @CircuitBreaker(name="productCatalog")
    public ProductSnapshot get(UUID productId) {
        try {
            var response=client.get().uri("/api/v1/products/{id}",productId).retrieve().body(ProductResponse.class);
            if(response==null) throw new ProductValidationException("Product service returned an empty response");
            return new ProductSnapshot(response.id(),response.sku(),response.name(),response.price(),response.currency(),response.active());
        } catch(RestClientException ex){throw new ProductValidationException("Product " + productId + " could not be validated",ex);}
    }
    record ProductResponse(UUID id,String sku,String name,String description,String category,BigDecimal price,String currency,boolean active,Instant createdAt,Instant updatedAt,long version){}
}
