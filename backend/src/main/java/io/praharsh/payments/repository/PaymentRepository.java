package io.praharsh.payments.repository;

import io.praharsh.payments.domain.Payment;
import io.praharsh.payments.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findAllByOrderByCreatedAtDesc();

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
}
