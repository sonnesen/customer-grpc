package com.sonnesen;

public record Customer(
    Long id,
    String name,
    String email,
    String phone,
    Address address,
    CustomerStatus status
) {

    public Customer withId(Long id) {
        return new Customer(id, name, email, phone, address, status);
    }
}
