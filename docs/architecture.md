# Architecture

## Boundaries

The repository has two independently built applications:

- `backend`: Spring Boot service owning payment state, persistence, event creation and API validation.
- `frontend`: Next.js console and same-origin route handlers. Only route handlers read `PAYMENTS_API_KEY`; client code calls `/api/...` without receiving the credential.

## Payment write path

1. The caller sends a tokenized request and an idempotency key.
2. `PaymentFingerprint` canonicalizes the financial fields and hashes them with SHA-256.
3. `PaymentService` returns the original payment for an identical replay or rejects a conflicting replay.
4. A new payment, its first timeline entry and an outbox event are written in one transaction.
5. A lifecycle change must include the visible payment version.
6. `PaymentStateMachine` validates the requested transition and any required reason.
7. JPA optimistic versioning and the explicit expected-version check prevent stale updates.

## Event path

`OutboxDispatcher` reads pending events in bounded batches. With the default profile it publishes directly to `PaymentEventStream`. With the `kafka` profile, `KafkaPaymentEventPublisher` waits for an acknowledged Kafka send before marking the row published; `KafkaPaymentEventListener` then forwards the event to the SSE stream.

The outbox table is the durable handoff from the payment transaction. The SSE connection is a view update channel, not a financial system of record.

## Persistence

Flyway owns the schema for:

- `payments`: current state, tokenized references, request fingerprint and optimistic version.
- `payment_transitions`: append-only status timeline.
- `outbox_events`: unpublished and published integration events.

The default H2 datasource runs in PostgreSQL compatibility mode. The `postgres` profile uses environment-supplied connection details without changing application code.

## Operability

- `/actuator/health`, liveness/readiness probes and metrics are enabled.
- Console logs use stable key/value fields for service, trace, logger and message.
- API errors use predictable codes for validation, missing resources, conflicts and invalid transitions.
- CI treats backend tests, frontend tests, static checks, dependency audit and container builds as release gates.
