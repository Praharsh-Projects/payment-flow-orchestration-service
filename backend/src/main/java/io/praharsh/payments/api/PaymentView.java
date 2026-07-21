package io.praharsh.payments.api;

import io.praharsh.payments.domain.Payment;
import io.praharsh.payments.domain.PaymentStatus;
import io.praharsh.payments.domain.PaymentTransition;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PaymentView(
        UUID id,
        String merchantReference,
        BigDecimal amount,
        String currency,
        String debtorAccountToken,
        String creditorAccountToken,
        PaymentStatus status,
        String statusReason,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<PaymentStatus> allowedTransitions,
        List<PaymentTransitionView> timeline
) {
    public static PaymentView from(
            Payment payment,
            List<PaymentTransition> transitions,
            Set<PaymentStatus> allowedTargets
    ) {
        return new PaymentView(
                payment.getId(),
                payment.getMerchantReference(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getDebtorAccountToken(),
                payment.getCreditorAccountToken(),
                payment.getStatus(),
                payment.getStatusReason(),
                payment.getVersion(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                allowedTargets.stream().sorted(Comparator.comparing(Enum::name)).toList(),
                transitions.stream().map(PaymentTransitionView::from).toList()
        );
    }
}
