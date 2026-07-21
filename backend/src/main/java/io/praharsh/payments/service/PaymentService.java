package io.praharsh.payments.service;

import io.praharsh.payments.api.CreatePaymentRequest;
import io.praharsh.payments.api.PaymentSummary;
import io.praharsh.payments.api.PaymentView;
import io.praharsh.payments.api.TransitionPaymentRequest;
import io.praharsh.payments.domain.OutboxEvent;
import io.praharsh.payments.domain.Payment;
import io.praharsh.payments.domain.PaymentStatus;
import io.praharsh.payments.domain.PaymentTransition;
import io.praharsh.payments.events.PaymentStatusEvent;
import io.praharsh.payments.repository.OutboxEventRepository;
import io.praharsh.payments.repository.PaymentRepository;
import io.praharsh.payments.repository.PaymentTransitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransitionRepository transitionRepository;
    private final OutboxEventRepository outboxRepository;
    private final PaymentStateMachine stateMachine;
    private final PaymentFingerprint fingerprint;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentTransitionRepository transitionRepository,
            OutboxEventRepository outboxRepository,
            PaymentStateMachine stateMachine,
            PaymentFingerprint fingerprint,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.paymentRepository = paymentRepository;
        this.transitionRepository = transitionRepository;
        this.outboxRepository = outboxRepository;
        this.stateMachine = stateMachine;
        this.fingerprint = fingerprint;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public CreatePaymentResult create(String idempotencyKey, CreatePaymentRequest request) {
        String requestFingerprint = fingerprint.calculate(request);
        var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (!payment.getRequestFingerprint().equals(requestFingerprint)) {
                throw new IdempotencyConflictException(idempotencyKey);
            }
            return new CreatePaymentResult(toView(payment), true);
        }

        Instant now = clock.instant();
        Payment payment = Payment.receive(
                idempotencyKey,
                requestFingerprint,
                request.merchantReference().trim(),
                request.amount(),
                request.currency().trim().toUpperCase(Locale.ROOT),
                request.debtorAccountToken().trim(),
                request.creditorAccountToken().trim(),
                now
        );
        paymentRepository.saveAndFlush(payment);

        PaymentTransition transition = new PaymentTransition(
                payment.getId(),
                null,
                PaymentStatus.RECEIVED,
                "Payment request accepted for processing.",
                payment.getVersion(),
                now
        );
        transitionRepository.save(transition);
        appendOutboxEvent(payment, now);

        return new CreatePaymentResult(toView(payment), false);
    }

    @Transactional
    public PaymentView transition(UUID paymentId, TransitionPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getVersion() != request.expectedVersion()) {
            throw new PaymentVersionConflictException(
                    paymentId,
                    request.expectedVersion(),
                    payment.getVersion()
            );
        }

        PaymentStatus previous = payment.getStatus();
        String reason = normalizeReason(request.reason());
        stateMachine.validate(previous, request.targetStatus(), reason);

        Instant now = clock.instant();
        payment.moveTo(request.targetStatus(), reason, now);
        paymentRepository.saveAndFlush(payment);

        transitionRepository.save(new PaymentTransition(
                payment.getId(),
                previous,
                payment.getStatus(),
                reason,
                payment.getVersion(),
                now
        ));
        appendOutboxEvent(payment, now);

        return toView(payment);
    }

    @Transactional(readOnly = true)
    public PaymentView get(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(this::toView)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    @Transactional(readOnly = true)
    public List<PaymentSummary> list(PaymentStatus status) {
        List<Payment> payments = status == null
                ? paymentRepository.findAllByOrderByCreatedAtDesc()
                : paymentRepository.findByStatusOrderByCreatedAtDesc(status);
        return payments.stream().map(PaymentSummary::from).toList();
    }

    private PaymentView toView(Payment payment) {
        return PaymentView.from(
                payment,
                transitionRepository.findByPaymentIdOrderBySequenceAsc(payment.getId()),
                stateMachine.allowedTargets(payment.getStatus())
        );
    }

    private void appendOutboxEvent(Payment payment, Instant occurredAt) {
        PaymentStatusEvent event = new PaymentStatusEvent(
                UUID.randomUUID(),
                payment.getId(),
                payment.getMerchantReference(),
                payment.getStatus(),
                payment.getVersion(),
                occurredAt
        );
        try {
            outboxRepository.save(new OutboxEvent(
                    payment.getId(),
                    "PAYMENT_%s".formatted(payment.getStatus()),
                    objectMapper.writeValueAsString(event),
                    occurredAt
            ));
        } catch (Exception exception) {
            throw new IllegalStateException("Payment event could not be serialized.", exception);
        }
    }

    private static String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }
}
