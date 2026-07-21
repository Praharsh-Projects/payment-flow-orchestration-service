package io.praharsh.payments.api;

import io.praharsh.payments.domain.Payment;
import io.praharsh.payments.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSummary(
        UUID id,
        String merchantReference,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String statusReason,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentSummary from(Payment payment) {
        return new PaymentSummary(
                payment.getId(),
                payment.getMerchantReference(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getStatusReason(),
                payment.getVersion(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
