package io.praharsh.payments;

import io.praharsh.payments.domain.PaymentStatus;
import io.praharsh.payments.service.InvalidPaymentTransitionException;
import io.praharsh.payments.service.PaymentStateMachine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStateMachineTest {

    private final PaymentStateMachine stateMachine = new PaymentStateMachine();

    @Test
    void exposesReceivedActions() {
        assertThat(stateMachine.allowedTargets(PaymentStatus.RECEIVED))
                .containsExactlyInAnyOrder(
                        PaymentStatus.AUTHORIZED,
                        PaymentStatus.REJECTED,
                        PaymentStatus.CANCELLED
                );
    }

    @Test
    void allowsAuthorizedPaymentToSettle() {
        stateMachine.validate(PaymentStatus.AUTHORIZED, PaymentStatus.SETTLED, null);
    }

    @Test
    void rejectsTransitionFromTerminalStatus() {
        assertThatThrownBy(() -> stateMachine.validate(
                PaymentStatus.SETTLED,
                PaymentStatus.CANCELLED,
                "operator request"
        )).isInstanceOf(InvalidPaymentTransitionException.class)
                .hasMessageContaining("SETTLED to CANCELLED");
    }

    @Test
    void requiresReasonForCancellation() {
        assertThatThrownBy(() -> stateMachine.validate(
                PaymentStatus.RECEIVED,
                PaymentStatus.CANCELLED,
                " "
        )).isInstanceOf(InvalidPaymentTransitionException.class)
                .hasMessageContaining("reason is required");
    }

    @Test
    void terminalStatusHasNoActions() {
        assertThat(stateMachine.allowedTargets(PaymentStatus.REJECTED)).isEmpty();
    }
}
