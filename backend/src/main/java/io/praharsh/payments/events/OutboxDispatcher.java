package io.praharsh.payments.events;

import io.praharsh.payments.config.PaymentProperties;
import io.praharsh.payments.domain.OutboxStatus;
import io.praharsh.payments.repository.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

@Component
@ConditionalOnProperty(name = "payments.outbox-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {

    private final OutboxEventRepository repository;
    private final PaymentEventPublisher publisher;
    private final PaymentProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxDispatcher(
            OutboxEventRepository repository,
            PaymentEventPublisher publisher,
            PaymentProperties properties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${payments.outbox-delay-ms:500}")
    @Transactional
    public void dispatchPending() {
        var events = repository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, properties.getOutboxBatchSize())
        );

        for (var event : events) {
            try {
                PaymentStatusEvent payload = objectMapper.readValue(event.getPayload(), PaymentStatusEvent.class);
                publisher.publish(payload);
                event.markPublished(clock.instant());
            } catch (Exception exception) {
                throw new IllegalStateException("Outbox event %s could not be published.".formatted(event.getId()), exception);
            }
        }
    }
}
