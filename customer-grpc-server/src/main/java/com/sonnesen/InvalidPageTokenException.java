package com.sonnesen;

public class InvalidPageTokenException extends IllegalArgumentException {

    public InvalidPageTokenException(String token, Throwable cause) {
        super("Invalid page_token: " + token, cause);
    }
}
