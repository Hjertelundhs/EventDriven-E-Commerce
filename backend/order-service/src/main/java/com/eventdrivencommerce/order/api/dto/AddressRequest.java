package com.eventdrivencommerce.order.api.dto;
import com.eventdrivencommerce.order.domain.model.Address;
import jakarta.validation.constraints.*;
public record AddressRequest(@NotBlank @Size(max=200) String recipient,@NotBlank @Size(max=255) String line1,@Size(max=255) String line2,@NotBlank @Size(max=32) String postalCode,@NotBlank @Size(max=120) String city,@NotBlank @Pattern(regexp="[A-Za-z]{2}") String countryCode){ public Address toDomain(){return new Address(recipient,line1,line2,postalCode,city,countryCode);} }
