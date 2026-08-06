package com.eventdrivencommerce.order.infrastructure.persistence;
import com.eventdrivencommerce.order.domain.model.Address;
import jakarta.persistence.Embeddable;
@Embeddable class AddressEmbeddable {
    String recipient; String line1; String line2; String postalCode; String city; String countryCode;
    protected AddressEmbeddable() {}
    static AddressEmbeddable from(Address a){var e=new AddressEmbeddable();e.recipient=a.recipient();e.line1=a.line1();e.line2=a.line2();e.postalCode=a.postalCode();e.city=a.city();e.countryCode=a.countryCode();return e;}
    Address toDomain(){return new Address(recipient,line1,line2,postalCode,city,countryCode);}
}
