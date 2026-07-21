package io.praharsh.payments;

import io.praharsh.payments.config.PaymentProperties;
import io.praharsh.payments.domain.PaymentStatus;
import io.praharsh.payments.events.KafkaPaymentEventPublisher;
import io.praharsh.payments.events.PaymentStatusEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaPaymentEventPublisherTest {

    @Test
    void publishesPaymentEventWithAggregateKey() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, PaymentStatusEvent> template = mock(KafkaTemplate.class);
        PaymentProperties properties = new PaymentProperties();
        properties.setEventsTopic("payment-status-events-test");
        PaymentStatusEvent event = new PaymentStatusEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "order-200",
                PaymentStatus.AUTHORIZED,
                1,
                Instant.parse("2026-07-21T10:00:00Z")
        );
        @SuppressWarnings("unchecked")
        SendResult<String, PaymentStatusEvent> result = mock(SendResult.class);
        when(template.send(
                "payment-status-events-test",
                event.paymentId().toString(),
                event
        )).thenReturn(CompletableFuture.completedFuture(result));

        new KafkaPaymentEventPublisher(template, properties).publish(event);

        verify(template).send(
                "payment-status-events-test",
                event.paymentId().toString(),
                event
        );
    }
}
