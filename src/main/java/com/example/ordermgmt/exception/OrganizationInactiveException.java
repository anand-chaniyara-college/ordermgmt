package com.example.ordermgmt.exception;

public class OrganizationInactiveException extends RuntimeException {
    public OrganizationInactiveException(String message) {
        super(message);
    }
}
