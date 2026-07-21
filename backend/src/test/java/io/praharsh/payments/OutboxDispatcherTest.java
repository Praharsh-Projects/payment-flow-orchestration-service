package io.praharsh.payments;

import io.praharsh.payments.config.PaymentProperties;
import io.praharsh.payments.domain.OutboxEvent;
import io.praharsh.payments.domain.OutboxStatus;
import io.praharsh.payments.domain.PaymentStatus;
import io.praharsh.payments.events.OutboxDispatcher;
import io.praharsh.payments.events.PaymentEventPublisher;
import io.praharsh.payments.events.PaymentStatusEvent;
import io.praharsh.payments.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxDispatcherTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-21T10:00:00Z");
    private static final Instant PUBLISHED_AT = Instant.parse("2026-07-21T10:00:01Z");

    @Test
    void publishesPendingEventAndMarksItComplete() throws Exception {
        Fixture fixture = fixture();

        fixture.dispatcher.dispatchPending();

        verify(fixture.publisher).publish(fixture.payload);
        assertThat(fixture.outboxEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(fixture.outboxEvent.getPublishedAt()).isEqualTo(PUBLISHED_AT);
    }

    @Test
    void leavesEventPendingWhenPublisherFails() throws Exception {
        Fixture fixture = fixture();
        doThrow(new IllegalStateException("broker unavailable"))
                .when(fixture.publisher).publish(fixture.payload);

        assertThatThrownBy(fixture.dispatcher::dispatchPending)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be published");
        assertThat(fixture.outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(fixture.outboxEvent.getPublishedAt()).isNull();
    }

    private static Fixture fixture() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentStatusEvent payload = new PaymentStatusEvent(
                UUID.randomUUID(),
                paymentId,
                "order-300",
                PaymentStatus.RECEIVED,
                0,
                CREATED_AT
        );
        OutboxEvent event = new OutboxEvent(paymentId, "PAYMENT_RECEIVED", "event-json", CREATED_AT);
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        PaymentEventPublisher publisher = mock(PaymentEventPublisher.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        PaymentProperties properties = new PaymentProperties();
        properties.setOutboxBatchSize(10);
        Clock clock = Clock.fixed(PUBLISHED_AT, ZoneOffset.UTC);

        when(repository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxStatus.PENDING),
                isA(Pageable.class)
        )).thenReturn(List.of(event));
        when(objectMapper.readValue("event-json", PaymentStatusEvent.class)).thenReturn(payload);

        return new Fixture(
                new OutboxDispatcher(repository, publisher, properties, objectMapper, clock),
                publisher,
                event,
                payload
        );
    }

    private record Fixture(
            OutboxDispatcher dispatcher,
            PaymentEventPublisher publisher,
            OutboxEvent outboxEvent,
            PaymentStatusEvent payload
    ) {
    }
}
