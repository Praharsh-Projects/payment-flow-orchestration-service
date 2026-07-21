package io.praharsh.payments.service;

import io.praharsh.payments.domain.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class PaymentStateMachine {

    private final Map<PaymentStatus, Set<PaymentStatus>> transitions = new EnumMap<>(PaymentStatus.class);

    public PaymentStateMachine() {
        transitions.put(
                PaymentStatus.RECEIVED,
                EnumSet.of(PaymentStatus.AUTHORIZED, PaymentStatus.REJECTED, PaymentStatus.CANCELLED)
        );
        transitions.put(
                PaymentStatus.AUTHORIZED,
                EnumSet.of(PaymentStatus.SETTLED, PaymentStatus.CANCELLED)
        );
        transitions.put(PaymentStatus.SETTLED, EnumSet.noneOf(PaymentStatus.class));
        transitions.put(PaymentStatus.REJECTED, EnumSet.noneOf(PaymentStatus.class));
        transitions.put(PaymentStatus.CANCELLED, EnumSet.noneOf(PaymentStatus.class));
    }

    public void validate(PaymentStatus current, PaymentStatus target, String reason) {
        if (!transitions.getOrDefault(current, Set.of()).contains(target)) {
            throw new InvalidPaymentTransitionException(
                    "Payment cannot move from %s to %s.".formatted(current, target)
            );
        }

        if ((target == PaymentStatus.REJECTED || target == PaymentStatus.CANCELLED)
                && (reason == null || reason.isBlank())) {
            throw new InvalidPaymentTransitionException(
                    "A reason is required when a payment is rejected or cancelled."
            );
        }
    }

    public Set<PaymentStatus> allowedTargets(PaymentStatus current) {
        return Set.copyOf(transitions.getOrDefault(current, Set.of()));
    }
}
