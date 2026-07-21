package io.praharsh.payments.service;

public class InvalidPaymentTransitionException extends RuntimeException {

    public InvalidPaymentTransitionException(String message) {
        super(message);
    }
}
