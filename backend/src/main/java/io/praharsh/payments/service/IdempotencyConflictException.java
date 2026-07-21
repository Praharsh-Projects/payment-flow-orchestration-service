package io.praharsh.payments.service;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String key) {
        super("Idempotency key '%s' was already used for a different payment request.".formatted(key));
    }
}
