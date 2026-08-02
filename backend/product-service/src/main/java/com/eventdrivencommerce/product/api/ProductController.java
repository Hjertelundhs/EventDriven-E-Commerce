package com.eventdrivencommerce.product.api;

import com.eventdrivencommerce.product.api.dto.CreateProductRequest;
import com.eventdrivencommerce.product.api.dto.ProductPageResponse;
import com.eventdrivencommerce.product.api.dto.ProductResponse;
import com.eventdrivencommerce.product.api.dto.UpdateProductRequest;
import com.eventdrivencommerce.product.application.model.CreateProductCommand;
import com.eventdrivencommerce.product.application.model.DeactivateProductCommand;
import com.eventdrivencommerce.product.application.model.PageResult;
import com.eventdrivencommerce.product.application.model.ProductResult;
import com.eventdrivencommerce.product.application.model.ProductSearchQuery;
import com.eventdrivencommerce.product.application.model.UpdateProductCommand;
import com.eventdrivencommerce.product.application.port.in.ProductCommandUseCase;
import com.eventdrivencommerce.product.application.port.in.ProductQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products")
public class ProductController {

    private final ProductCommandUseCase commands;
    private final ProductQueryUseCase queries;
    private final ProductApiMapper mapper;

    public ProductController(ProductCommandUseCase commands, ProductQueryUseCase queries, ProductApiMapper mapper) {
        this.commands = commands;
        this.queries = queries;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Create a product", description = "Administrative authorization is enforced in Phase 8")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request,
                                                   HttpServletRequest servletRequest) {
        RequestIds ids = requestIds(servletRequest);
        ProductResponse response = mapper.toResponse(commands.create(new CreateProductCommand(
                request.sku(), request.name(), request.description(), request.category(), request.price(),
                request.currency(), ids.correlationId(), ids.causationId()
        )));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).eTag(etag(response.version())).body(response);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update a product")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID productId,
                                                   @Valid @RequestBody UpdateProductRequest request,
                                                   HttpServletRequest servletRequest) {
        RequestIds ids = requestIds(servletRequest);
        ProductResponse response = mapper.toResponse(commands.update(new UpdateProductCommand(
                productId, request.sku(), request.name(), request.description(), request.category(), request.price(),
                request.currency(), request.version(), ids.correlationId(), ids.causationId()
        )));
        return ResponseEntity.ok().eTag(etag(response.version())).body(response);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Deactivate a product")
    public ResponseEntity<ProductResponse> deactivate(@PathVariable UUID productId,
                                                       @RequestParam @PositiveOrZero long version,
                                                       HttpServletRequest servletRequest) {
        RequestIds ids = requestIds(servletRequest);
        ProductResponse response = mapper.toResponse(commands.deactivate(new DeactivateProductCommand(
                productId, version, ids.correlationId(), ids.causationId()
        )));
        return ResponseEntity.ok().eTag(etag(response.version())).body(response);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get a product")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID productId) {
        ProductResponse response = mapper.toResponse(queries.get(productId));
        return ResponseEntity.ok().eTag(etag(response.version())).body(response);
    }

    @GetMapping
    @Operation(summary = "Search and list products")
    public ProductPageResponse search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        ProductSearchQuery query = new ProductSearchQuery(name, category, active, page, size,
                ProductSearchQuery.SortField.parse(sort), ProductSearchQuery.SortDirection.parse(direction));
        PageResult<ProductResult> result = queries.search(query);
        return new ProductPageResponse(result.items().stream().map(mapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(), result.first(), result.last());
    }

    private static RequestIds requestIds(HttpServletRequest request) {
        return new RequestIds((UUID) request.getAttribute(CorrelationIdFilter.CORRELATION_ATTRIBUTE),
                (UUID) request.getAttribute(CorrelationIdFilter.CAUSATION_ATTRIBUTE));
    }

    private static String etag(long version) {
        return "\"" + version + "\"";
    }

    private record RequestIds(UUID correlationId, UUID causationId) {}
}
