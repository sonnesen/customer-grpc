package com.sonnesen;

import com.sonnesen.customer.grpc.CreateCustomerRequest;

/**
 * Converts between the wire representation generated from {@code customer.proto}
 * (package {@code com.sonnesen.customer.grpc}) and the domain model used internally
 * by the server (package {@code com.sonnesen}).
 */
public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static Customer toDomain(CreateCustomerRequest request) {
        return new Customer(
            null,
            request.getName(),
            request.getEmail(),
            request.getPhone(),
            toDomain(request.getAddress()),
            CustomerStatus.ACTIVE
        );
    }

    public static Address toDomain(com.sonnesen.customer.grpc.Address address) {
        return new Address(
            address.getStreet(),
            address.getCity(),
            address.getState(),
            address.getZipCode()
        );
    }

    public static com.sonnesen.customer.grpc.Customer toProto(Customer customer) {
        return com.sonnesen.customer.grpc.Customer.newBuilder()
            .setId(customer.id())
            .setName(customer.name())
            .setEmail(customer.email())
            .setPhone(customer.phone())
            .setAddress(toProto(customer.address()))
            .setStatus(toProto(customer.status()))
            .build();
    }

    public static com.sonnesen.customer.grpc.Address toProto(Address address) {
        return com.sonnesen.customer.grpc.Address.newBuilder()
            .setStreet(address.street())
            .setCity(address.city())
            .setState(address.state())
            .setZipCode(address.zipCode())
            .build();
    }

    public static com.sonnesen.customer.grpc.CustomerStatus toProto(CustomerStatus status) {
        return switch (status) {
            case ACTIVE -> com.sonnesen.customer.grpc.CustomerStatus.CUSTOMER_STATUS_ACTIVE;
            case INACTIVE -> com.sonnesen.customer.grpc.CustomerStatus.CUSTOMER_STATUS_INACTIVE;
        };
    }

    public static CustomerStatus toDomain(com.sonnesen.customer.grpc.CustomerStatus status) {
        return switch (status) {
            case CUSTOMER_STATUS_INACTIVE -> CustomerStatus.INACTIVE;
            case CUSTOMER_STATUS_ACTIVE, CUSTOMER_STATUS_UNSPECIFIED, UNRECOGNIZED -> CustomerStatus.ACTIVE;
        };
    }
}
