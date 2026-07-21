package io.praharsh.payments.events;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!kafka")
public class InProcessPaymentEventPublisher implements PaymentEventPublisher {

    private final PaymentEventStream eventStream;

    public InProcessPaymentEventPublisher(PaymentEventStream eventStream) {
        this.eventStream = eventStream;
    }

    @Override
    public void publish(PaymentStatusEvent event) {
        eventStream.broadcast(event);
    }
}
