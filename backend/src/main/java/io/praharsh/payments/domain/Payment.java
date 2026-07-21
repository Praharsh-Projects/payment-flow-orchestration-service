package io.praharsh.payments.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "merchant_reference", nullable = false, length = 64)
    private String merchantReference;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "debtor_account_token", nullable = false, length = 64)
    private String debtorAccountToken;

    @Column(name = "creditor_account_token", nullable = false, length = 64)
    private String creditorAccountToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "status_reason", length = 160)
    private String statusReason;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    private Payment(
            UUID id,
            String idempotencyKey,
            String requestFingerprint,
            String merchantReference,
            BigDecimal amount,
            String currency,
            String debtorAccountToken,
            String creditorAccountToken,
            Instant now
    ) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.merchantReference = merchantReference;
        this.amount = amount;
        this.currency = currency;
        this.debtorAccountToken = debtorAccountToken;
        this.creditorAccountToken = creditorAccountToken;
        this.status = PaymentStatus.RECEIVED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Payment receive(
            String idempotencyKey,
            String requestFingerprint,
            String merchantReference,
            BigDecimal amount,
            String currency,
            String debtorAccountToken,
            String creditorAccountToken,
            Instant now
    ) {
        return new Payment(
                UUID.randomUUID(),
                idempotencyKey,
                requestFingerprint,
                merchantReference,
                amount,
                currency,
                debtorAccountToken,
                creditorAccountToken,
                now
        );
    }

    public void moveTo(PaymentStatus targetStatus, String reason, Instant now) {
        this.status = targetStatus;
        this.statusReason = reason;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDebtorAccountToken() {
        return debtorAccountToken;
    }

    public String getCreditorAccountToken() {
        return creditorAccountToken;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
