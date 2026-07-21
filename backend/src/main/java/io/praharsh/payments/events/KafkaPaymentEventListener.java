package io.praharsh.payments.events;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("kafka")
public class KafkaPaymentEventListener {

    private final PaymentEventStream eventStream;

    public KafkaPaymentEventListener(PaymentEventStream eventStream) {
        this.eventStream = eventStream;
    }

    @KafkaListener(topics = "${payments.events-topic}", groupId = "payment-console")
    public void onPaymentStatus(PaymentStatusEvent event) {
        eventStream.broadcast(event);
    }
}
