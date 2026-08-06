package com.eventdrivencommerce.order.api.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
public record CreateOrderRequest(@NotEmpty @Size(max=100) List<@Valid RequestedLine> lines,@NotBlank @Pattern(regexp="[A-Za-z]{3}") String currency,@NotNull @Valid AddressRequest shippingAddress,@NotNull @Valid AddressRequest billingAddress){ public record RequestedLine(@NotNull UUID productId,@Min(1) @Max(10000) int quantity){} }
