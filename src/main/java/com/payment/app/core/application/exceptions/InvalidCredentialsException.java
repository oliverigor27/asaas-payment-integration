package com.payment.app.core.application.exceptions;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Usuário ou senha inválidos");
    }
}
