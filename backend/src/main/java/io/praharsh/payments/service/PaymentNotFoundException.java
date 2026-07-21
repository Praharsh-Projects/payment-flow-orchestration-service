package io.praharsh.payments.service;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID paymentId) {
        super("Payment %s was not found.".formatted(paymentId));
    }
}
