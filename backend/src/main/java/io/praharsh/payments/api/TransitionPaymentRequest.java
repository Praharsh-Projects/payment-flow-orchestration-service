package io.praharsh.payments.api;

import io.praharsh.payments.domain.PaymentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransitionPaymentRequest(
        @NotNull PaymentStatus targetStatus,
        @Min(0) long expectedVersion,
        @Size(max = 160) String reason
) {
}
