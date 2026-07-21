package io.praharsh.payments.api;

import io.praharsh.payments.domain.PaymentStatus;
import io.praharsh.payments.domain.PaymentTransition;

import java.time.Instant;

public record PaymentTransitionView(
        PaymentStatus fromStatus,
        PaymentStatus toStatus,
        String reason,
        long sequence,
        Instant occurredAt
) {
    public static PaymentTransitionView from(PaymentTransition transition) {
        return new PaymentTransitionView(
                transition.getFromStatus(),
                transition.getToStatus(),
                transition.getReason(),
                transition.getSequence(),
                transition.getOccurredAt()
        );
    }
}
