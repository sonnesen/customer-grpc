package com.sonnesen;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("A customer with email '" + email + "' already exists");
    }
}
