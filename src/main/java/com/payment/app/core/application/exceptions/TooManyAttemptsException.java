package com.payment.app.core.application.exceptions;

import java.util.concurrent.TimeUnit;

public class TooManyAttemptsException extends RuntimeException {
    private final long retryAfterSeconds;

    public TooManyAttemptsException(long nanosToWait) {
        super("Muitas tentativas. Tente novamente mais tarde.");
        this.retryAfterSeconds = Math.max(4, TimeUnit.NANOSECONDS.toSeconds(nanosToWait) + 1);
    }

    public long getRetryAgterSeconds() { return retryAfterSeconds; }
}
