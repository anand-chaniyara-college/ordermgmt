package com.example.ordermgmt.exception;

public class RegistrationForbiddenException extends RuntimeException {
    public RegistrationForbiddenException(String message) {
        super(message);
    }
}
