package io.praharsh.payments.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transitions")
public class PaymentTransition {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private PaymentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private PaymentStatus toStatus;

    @Column(length = 160)
    private String reason;

    @Column(nullable = false)
    private long sequence;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected PaymentTransition() {
    }

    public PaymentTransition(
            UUID paymentId,
            PaymentStatus fromStatus,
            PaymentStatus toStatus,
            String reason,
            long sequence,
            Instant occurredAt
    ) {
        this.id = UUID.randomUUID();
        this.paymentId = paymentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.sequence = sequence;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getFromStatus() {
        return fromStatus;
    }

    public PaymentStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public long getSequence() {
        return sequence;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
