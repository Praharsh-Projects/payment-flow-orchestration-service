package io.praharsh.payments.domain;

public enum PaymentStatus {
    RECEIVED,
    AUTHORIZED,
    SETTLED,
    REJECTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == SETTLED || this == REJECTED || this == CANCELLED;
    }
}
