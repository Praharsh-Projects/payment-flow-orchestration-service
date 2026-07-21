package io.praharsh.payments;

import io.praharsh.payments.domain.PaymentStatus;
import io.praharsh.payments.events.KafkaPaymentEventPublisher;
import io.praharsh.payments.events.PaymentStatusEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(
        partitions = 1,
        topics = KafkaProfileIntegrationTest.TOPIC,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@SpringBootTest(properties = {
        "spring.profiles.active=kafka",
        "payments.events-topic=" + KafkaProfileIntegrationTest.TOPIC,
        "payments.outbox-enabled=false"
})
class KafkaProfileIntegrationTest {

    static final String TOPIC = "embedded-payment-status-events";

    @Autowired
    private KafkaPaymentEventPublisher publisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void publishesJsonEventThroughEmbeddedKafkaBroker() {
        var consumerProperties = KafkaTestUtils.consumerProps(embeddedKafka, "verification-observer", true);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        var consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProperties,
                new StringDeserializer(),
                new StringDeserializer()
        );

        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, TOPIC);
            PaymentStatusEvent event = new PaymentStatusEvent(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "embedded-kafka-order",
                    PaymentStatus.AUTHORIZED,
                    1,
                    Instant.parse("2026-07-21T10:00:00Z")
            );

            publisher.publish(event);
            var record = KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10));

            assertThat(record.key()).isEqualTo(event.paymentId().toString());
            assertThat(record.value()).contains(
                    "embedded-kafka-order",
                    "AUTHORIZED",
                    event.paymentId().toString()
            );
        }
    }
}
