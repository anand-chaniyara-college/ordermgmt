package com.example.ordermgmt.exception;

public class AccountInactiveException extends AuthException {
    public AccountInactiveException(String message) {
        super(message);
    }
}
