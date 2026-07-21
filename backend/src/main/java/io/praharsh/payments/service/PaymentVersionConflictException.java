package io.praharsh.payments.service;

import java.util.UUID;

public class PaymentVersionConflictException extends RuntimeException {

    public PaymentVersionConflictException(UUID paymentId, long expected, long actual) {
        super("Payment %s changed after it was loaded (expected version %d, current version %d). Refresh before retrying."
                .formatted(paymentId, expected, actual));
    }
}
