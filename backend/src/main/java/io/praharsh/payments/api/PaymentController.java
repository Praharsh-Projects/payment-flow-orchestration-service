package io.praharsh.payments.api;

import io.praharsh.payments.domain.PaymentStatus;
import io.praharsh.payments.events.PaymentEventStream;
import io.praharsh.payments.service.CreatePaymentResult;
import io.praharsh.payments.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService service;
    private final PaymentEventStream eventStream;

    public PaymentController(PaymentService service, PaymentEventStream eventStream) {
        this.service = service;
        this.eventStream = eventStream;
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentView> create(
            @RequestHeader("Idempotency-Key")
            @Size(min = 8, max = 80)
            @Pattern(regexp = "^[A-Za-z0-9._:-]+$")
            String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        CreatePaymentResult result = service.create(idempotencyKey, request);
        if (result.replayed()) {
            return ResponseEntity.ok()
                    .header("Idempotency-Replayed", "true")
                    .body(result.payment());
        }
        return ResponseEntity.created(URI.create("/api/v1/payments/" + result.payment().id()))
                .header("Idempotency-Replayed", "false")
                .body(result.payment());
    }

    @GetMapping("/payments")
    public List<PaymentSummary> list(@RequestParam(required = false) PaymentStatus status) {
        return service.list(status);
    }

    @GetMapping("/payments/{paymentId}")
    public PaymentView get(@PathVariable UUID paymentId) {
        return service.get(paymentId);
    }

    @PostMapping("/payments/{paymentId}/transitions")
    public PaymentView transition(
            @PathVariable UUID paymentId,
            @Valid @RequestBody TransitionPaymentRequest request
    ) {
        return service.transition(paymentId, request);
    }

    @GetMapping(path = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return eventStream.subscribe();
    }
}
