package com.example.ordermgmt.exception;

public class RoleNotFoundException extends AuthException {
    public RoleNotFoundException(String message) {
        super(message);
    }
}
