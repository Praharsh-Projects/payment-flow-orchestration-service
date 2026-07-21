package io.praharsh.payments.events;

public interface PaymentEventPublisher {

    void publish(PaymentStatusEvent event);
}
