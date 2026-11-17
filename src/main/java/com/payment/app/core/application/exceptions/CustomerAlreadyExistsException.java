package com.payment.app.core.application.exceptions;

public class CustomerAlreadyExistsException extends RuntimeException {
    public CustomerAlreadyExistsException() {
        super("Customer already exists!");
    }
}
