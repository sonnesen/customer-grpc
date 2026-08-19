package com.sonnesen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sonnesen.customer.grpc.CreateCustomerRequest;

class CustomerMapperTest {

    private static final com.sonnesen.customer.grpc.Address PROTO_ADDRESS =
        com.sonnesen.customer.grpc.Address.newBuilder()
            .setStreet("Rua das Flores, 123")
            .setCity("Porto Alegre")
            .setState("RS")
            .setZipCode("90000-000")
            .build();

    @Test
    void mapsCreateCustomerRequestToDomainWithActiveStatusAndNoId() {
        CreateCustomerRequest request = CreateCustomerRequest.newBuilder()
            .setName("Ada Lovelace")
            .setEmail("ada@example.com")
            .setPhone("+55 51 90000-0000")
            .setAddress(PROTO_ADDRESS)
            .build();

        Customer domain = CustomerMapper.toDomain(request);

        assertEquals(null, domain.id());
        assertEquals("Ada Lovelace", domain.name());
        assertEquals("ada@example.com", domain.email());
        assertEquals("+55 51 90000-0000", domain.phone());
        assertEquals(CustomerStatus.ACTIVE, domain.status());
        assertEquals("Porto Alegre", domain.address().city());
    }

    @Test
    void roundTripsDomainCustomerThroughProtoAndBack() {
        Customer original = new Customer(
            42L,
            "Grace Hopper",
            "grace@example.com",
            "+1 555-0100",
            new Address("1 Infinite Loop", "Arlington", "VA", "22201"),
            CustomerStatus.INACTIVE
        );

        com.sonnesen.customer.grpc.Customer proto = CustomerMapper.toProto(original);
        assertEquals(42L, proto.getId());
        assertEquals(com.sonnesen.customer.grpc.CustomerStatus.CUSTOMER_STATUS_INACTIVE, proto.getStatus());

        Customer roundTripped = new Customer(
            proto.getId(),
            proto.getName(),
            proto.getEmail(),
            proto.getPhone(),
            CustomerMapper.toDomain(proto.getAddress()),
            CustomerMapper.toDomain(proto.getStatus())
        );

        assertEquals(original, roundTripped);
    }
}
