# Payment Flow Orchestration Service

Full-stack reference implementation for controlled payment lifecycle operations. A Java and Spring Boot API accepts synthetic payment instructions, enforces idempotency and version checks, records every state change, and emits events through a transactional outbox. A Next.js and Tailwind console exposes the queue, allowed actions and audit timeline without sending the backend API key to the browser.

This repository models engineering controls around a payment flow. It does not connect to payment rails or move real funds.

## System behavior

- Accepts a payment instruction only when account references use the `acct_...` token format.
- Requires a caller-supplied idempotency key and returns the original payment for an identical replay.
- Rejects reuse of an idempotency key with different financial fields.
- Allows only explicit lifecycle transitions: `RECEIVED -> AUTHORIZED -> SETTLED`, with controlled rejection or cancellation paths.
- Uses optimistic versions so a stale operator action returns `409 Conflict` instead of overwriting a newer state.
- Writes the payment, transition audit entry and outbox event in one database transaction.
- Publishes outbox events in-process by default or through Kafka when the `kafka` profile is active.
- Streams payment-status events to the web console through server-sent events.
- Keeps the backend API key in Next.js route handlers rather than client-side JavaScript.

## Architecture

```text
Browser
  Next.js + TypeScript + Tailwind console
               |
               | same-origin requests
               v
  Next.js route handlers (server-held API key)
               |
               | JSON + SSE
               v
  Spring Boot REST API
    | Payment service + state machine
    | JPA + Flyway
    | transactional outbox
    +---------------------> Kafka profile -> SSE listener
    |                                    
    +-> H2 local / PostgreSQL runtime -> audit timeline
```

See [docs/architecture.md](docs/architecture.md), [docs/api.md](docs/api.md) and [docs/security-and-limitations.md](docs/security-and-limitations.md).

## Technology

- Java 17, Spring Boot 4.0.5, Gradle 9.4.1
- Spring MVC, Validation, Data JPA, Flyway, Actuator and Spring Kafka
- H2 for zero-setup review and PostgreSQL for the persistent profile
- Next.js 15, React 19, TypeScript and Tailwind CSS
- Vitest, Testing Library, JUnit, AssertJ, Mockito and JaCoCo
- Docker Compose, GitHub Actions and Dependabot

## Quick start

Requirements: Java 17, Node.js 20.19 or later, and npm.

Start the API with its in-memory database and in-process event publisher:

```bash
cd backend
./gradlew bootRun
```

In another terminal, start the console:

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000`. The local API key defaults to `local-review-key`. Override it in both processes with `PAYMENTS_API_KEY` outside local review.

### Full PostgreSQL and Kafka topology

```bash
docker compose up --build
```

The Compose topology starts PostgreSQL, Redpanda's Kafka-compatible broker, the Spring Boot service with `postgres,kafka` profiles, and the Next.js console.

## API example

```bash
curl -i http://localhost:8080/api/v1/payments \
  -H 'X-API-Key: local-review-key' \
  -H 'Idempotency-Key: terminal-order-2026-001' \
  -H 'Content-Type: application/json' \
  -d '{
    "merchantReference": "order-2026-001",
    "amount": 1250.50,
    "currency": "SEK",
    "debtorAccountToken": "acct_debtor_01",
    "creditorAccountToken": "acct_creditor_01"
  }'
```

Authorize the returned payment with the version shown in the response:

```bash
curl -i http://localhost:8080/api/v1/payments/PAYMENT_ID/transitions \
  -H 'X-API-Key: local-review-key' \
  -H 'Content-Type: application/json' \
  -d '{"targetStatus":"AUTHORIZED","expectedVersion":0,"reason":null}'
```

## Quality gates

Run the same local checks used during implementation:

```bash
make quality
```

Verified locally on 26 July 2026:

- 22 backend tests passed.
- Backend JaCoCo line coverage: 88.19%; instruction coverage: 87.68%.
- 10 frontend tests passed.
- Frontend statement coverage: 84.61%; line coverage: 90.10%.
- Biome lint, TypeScript checking and the Next.js production build passed.
- `npm audit --audit-level=moderate` reported 0 vulnerabilities after updating the patched Next.js 15 release and transitive image, CSS, and glob-processing dependencies.

The CI workflow repeats backend tests, frontend checks, coverage, production build, dependency audit, Compose validation and both container builds.

## AI-assisted development workflow

The implementation used an agentic coding workflow for scaffolding, test generation and review suggestions. Every accepted change was checked through compilation, tests, coverage, dependency audit and manual source review. No productivity percentage or autonomous production delivery is claimed. See [docs/engineering-workflow.md](docs/engineering-workflow.md).

## Limits

- All identifiers, amounts and actions are synthetic.
- There is no bank, card, clearing, settlement or customer integration.
- Kafka dispatch waits for broker acknowledgement, but the example does not implement multi-region delivery, dead-letter queues or operational replay tooling.
- The API-key mechanism is a narrow service boundary, not a replacement for production identity, authorization, key rotation or mTLS.
- No payment certification, regulatory approval, production deployment, performance benchmark or user-scale claim is made.
