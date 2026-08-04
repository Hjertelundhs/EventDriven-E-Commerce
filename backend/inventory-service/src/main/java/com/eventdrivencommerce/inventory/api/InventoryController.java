package com.eventdrivencommerce.inventory.api;

import com.eventdrivencommerce.inventory.api.dto.AdjustStockRequest;
import com.eventdrivencommerce.inventory.api.dto.InventoryResponse;
import com.eventdrivencommerce.inventory.api.dto.ReceiveStockRequest;
import com.eventdrivencommerce.inventory.api.dto.ReservationResponse;
import com.eventdrivencommerce.inventory.api.dto.ReservationTransitionRequest;
import com.eventdrivencommerce.inventory.api.dto.ReserveStockRequest;
import com.eventdrivencommerce.inventory.application.model.AdjustStockCommand;
import com.eventdrivencommerce.inventory.application.model.ChangeReservationCommand;
import com.eventdrivencommerce.inventory.application.model.ReceiveStockCommand;
import com.eventdrivencommerce.inventory.application.model.ReserveStockCommand;
import com.eventdrivencommerce.inventory.application.model.ReserveStockResult;
import com.eventdrivencommerce.inventory.application.port.in.InventoryCommandUseCase;
import com.eventdrivencommerce.inventory.application.port.in.InventoryQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory")
public class InventoryController {

    private static final String SKU_PATTERN = "(?i)[A-Z0-9][A-Z0-9._-]{2,63}";

    private final InventoryCommandUseCase commands;
    private final InventoryQueryUseCase queries;
    private final InventoryApiMapper mapper;

    public InventoryController(InventoryCommandUseCase commands, InventoryQueryUseCase queries,
                               InventoryApiMapper mapper) {
        this.commands = commands;
        this.queries = queries;
        this.mapper = mapper;
    }

    @GetMapping("/{sku}")
    @Operation(summary = "Get stock status for a SKU")
    public ResponseEntity<InventoryResponse> getStock(
            @PathVariable @Pattern(regexp = SKU_PATTERN) String sku) {
        InventoryResponse response = mapper.toResponse(queries.getStock(sku));
        return ResponseEntity.ok().eTag(etag(response.version())).body(response);
    }

    @PostMapping("/{sku}/receipts")
    @Operation(summary = "Receive stock")
    public ResponseEntity<InventoryResponse> receive(
            @PathVariable @Pattern(regexp = SKU_PATTERN) String sku,
            @Valid @RequestBody ReceiveStockRequest request) {
        InventoryResponse response = mapper.toResponse(commands.receive(
                new ReceiveStockCommand(sku, request.quantity(), request.version())));
        return ResponseEntity.ok().eTag(etag(response.version())).body(response);
    }

    @PutMapping("/{sku}/adjustment")
    @Operation(summary = "Set the physical total stock quantity")
    public ResponseEntity<InventoryResponse> adjust(
            @PathVariable @Pattern(regexp = SKU_PATTERN) String sku,
            @Valid @RequestBody AdjustStockRequest request) {
        InventoryResponse response = mapper.toResponse(commands.adjust(
                new AdjustStockCommand(sku, request.totalQuantity(), request.version())));
        return ResponseEntity.ok().eTag(etag(response.version())).body(response);
    }

    @PostMapping("/reservations")
    @Operation(summary = "Reserve stock for an order")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveStockRequest request) {
        ReserveStockResult result = commands.reserve(new ReserveStockCommand(
                request.orderId(), request.sku(), request.quantity(), request.inventoryVersion()));
        ReservationResponse response = mapper.toResponse(result.reservation());
        ResponseEntity.BodyBuilder builder;
        if (result.created()) {
            URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                    .buildAndExpand(response.id()).toUri();
            builder = ResponseEntity.created(location);
        } else {
            builder = ResponseEntity.ok();
        }
        return builder.eTag(etag(response.version())).body(response);
    }

    @GetMapping("/reservations/{reservationId}")
    @Operation(summary = "Get a reservation")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable UUID reservationId) {
        ReservationResponse response = mapper.toResponse(queries.getReservation(reservationId));
        return ResponseEntity.ok().eTag(etag(response.version())).body(response);
    }

    @PostMapping("/reservations/{reservationId}/release")
    @Operation(summary = "Release a reservation back to available stock")
    public ResponseEntity<ReservationResponse> release(
            @PathVariable UUID reservationId,
            @Valid @RequestBody ReservationTransitionRequest request) {
        ReservationResponse response = mapper.toResponse(commands.release(new ChangeReservationCommand(
                reservationId, request.reservationVersion(), request.inventoryVersion())));
        return ResponseEntity.ok().eTag(etag(response.version())).body(response);
    }

    @PostMapping("/reservations/{reservationId}/completion")
    @Operation(summary = "Complete a reservation and consume its stock")
    public ResponseEntity<ReservationResponse> complete(
            @PathVariable UUID reservationId,
            @Valid @RequestBody ReservationTransitionRequest request) {
        ReservationResponse response = mapper.toResponse(commands.complete(new ChangeReservationCommand(
                reservationId, request.reservationVersion(), request.inventoryVersion())));
        return ResponseEntity.ok().eTag(etag(response.version())).body(response);
    }

    private static String etag(long version) {
        return "\"" + version + "\"";
    }
}
