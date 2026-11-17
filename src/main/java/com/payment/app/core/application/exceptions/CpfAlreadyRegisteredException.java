package com.payment.app.core.application.exceptions;

public class CpfAlreadyRegisteredException extends RuntimeException {
    public CpfAlreadyRegisteredException() {
        super("CPF already exists!");
    }
}
