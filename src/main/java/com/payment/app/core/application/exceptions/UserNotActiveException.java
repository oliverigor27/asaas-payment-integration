package com.payment.app.core.application.exceptions;

public class UserNotActiveException extends RuntimeException {
    public UserNotActiveException() {
        super("Usr is not active. He cannot create a customer.");
    }
}
