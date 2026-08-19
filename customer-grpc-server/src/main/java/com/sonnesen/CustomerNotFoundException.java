package com.sonnesen;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(long id) {
        super("Customer not found: " + id);
    }
}
