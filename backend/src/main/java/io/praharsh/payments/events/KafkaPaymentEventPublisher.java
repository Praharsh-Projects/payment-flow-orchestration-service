package io.praharsh.payments.events;

import io.praharsh.payments.config.PaymentProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Profile("kafka")
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    private final KafkaTemplate<String, PaymentStatusEvent> kafkaTemplate;
    private final PaymentProperties properties;

    public KafkaPaymentEventPublisher(
            KafkaTemplate<String, PaymentStatusEvent> kafkaTemplate,
            PaymentProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(PaymentStatusEvent event) {
        try {
            kafkaTemplate.send(
                    properties.getEventsTopic(),
                    event.paymentId().toString(),
                    event
            ).get(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka did not acknowledge the payment event.", exception);
        }
    }
}
