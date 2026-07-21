package io.praharsh.payments.events;

import io.praharsh.payments.domain.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentStatusEvent(
        UUID eventId,
        UUID paymentId,
        String merchantReference,
        PaymentStatus status,
        long version,
        Instant occurredAt
) {
}
