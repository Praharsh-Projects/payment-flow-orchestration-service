package io.praharsh.payments.repository;

import io.praharsh.payments.domain.PaymentTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentTransitionRepository extends JpaRepository<PaymentTransition, UUID> {

    List<PaymentTransition> findByPaymentIdOrderBySequenceAsc(UUID paymentId);
}
