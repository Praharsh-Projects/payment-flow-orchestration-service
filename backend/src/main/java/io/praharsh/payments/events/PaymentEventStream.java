package io.praharsh.payments.events;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class PaymentEventStream {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ignored -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("payment-stream-ready"));
        } catch (IOException exception) {
            emitters.remove(emitter);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public void broadcast(PaymentStatusEvent event) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.eventId().toString())
                        .name("payment-status")
                        .data(event));
            } catch (IOException exception) {
                emitters.remove(emitter);
                emitter.completeWithError(exception);
            }
        }
    }
}
