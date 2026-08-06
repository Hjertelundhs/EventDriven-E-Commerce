package com.eventdrivencommerce.order.api;

import com.eventdrivencommerce.order.api.dto.*;
import com.eventdrivencommerce.order.application.model.CreateOrderCommand;
import com.eventdrivencommerce.order.application.port.in.OrderUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Validated @RestController @RequestMapping("/api/v1")
public class OrderController {
    private final OrderUseCase orders;private final ObjectMapper mapper;private final OrderStatusStream stream;
    public OrderController(OrderUseCase orders,ObjectMapper mapper,OrderStatusStream stream){this.orders=orders;this.mapper=mapper;this.stream=stream;}
    @PostMapping("/orders") @Operation(summary="Register an order and start its saga")
    public ResponseEntity<OrderResponse> create(@RequestHeader("X-Customer-ID") UUID customerId,@RequestHeader("Idempotency-Key") @NotBlank String key,@Valid @RequestBody CreateOrderRequest request,HttpServletRequest servletRequest){
        if(key.length()>120)throw new IllegalArgumentException("Idempotency-Key cannot exceed 120 characters");
        var command=new CreateOrderCommand(customerId,request.lines().stream().map(l->new CreateOrderCommand.RequestedLine(l.productId(),l.quantity())).toList(),request.currency().toUpperCase(),request.shippingAddress().toDomain(),request.billingAddress().toDomain(),key,fingerprint(request),UUID.fromString((String)servletRequest.getAttribute(CorrelationIdFilter.HEADER)));
        var result=orders.create(command);var response=OrderResponse.from(result.order());
        return ResponseEntity.status(result.replayed()?HttpStatus.OK:HttpStatus.ACCEPTED).location(URI.create("/api/v1/orders/"+response.id())).eTag('"'+Long.toString(response.version())+'"').body(response);
    }
    @GetMapping("/orders/{id}") public ResponseEntity<OrderResponse> get(@PathVariable UUID id,@RequestHeader("X-Customer-ID")UUID customerId){var o=orders.get(id,customerId);return ResponseEntity.ok().eTag('"'+Long.toString(o.version())+'"').body(OrderResponse.from(o));}
    @GetMapping("/customers/me/orders") public Page<OrderResponse> list(@RequestHeader("X-Customer-ID")UUID customerId,@PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC)Pageable pageable){return orders.list(customerId,pageable).map(OrderResponse::from);}
    @GetMapping(path="/orders/{id}/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE) public SseEmitter events(@PathVariable UUID id,@RequestHeader("X-Customer-ID")UUID customerId){return stream.subscribe(orders.get(id,customerId));}
    private String fingerprint(CreateOrderRequest request){try{byte[] data=mapper.writeValueAsBytes(request);return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}catch(JsonProcessingException|NoSuchAlgorithmException ex){throw new IllegalStateException("Could not fingerprint request",ex);}}
}
