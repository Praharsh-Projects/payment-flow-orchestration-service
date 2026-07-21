# Security controls and limitations

## Implemented controls

- Constant-time comparison for the service API key.
- Server-side Next.js proxy prevents the API key from being embedded in the browser bundle.
- Strict request validation and token-only account-reference format.
- Idempotency fingerprint over the financial request fields.
- Explicit lifecycle state machine and mandatory reasons for exception outcomes.
- Expected-version checks plus JPA optimistic locking.
- Transactional outbox and append-only transition timeline.
- Structured error codes that avoid stack traces and database details.
- Non-root users in backend and frontend runtime images.
- Dependency audit and container builds in CI.

## Production requirements not implemented

- OAuth/OIDC, scoped authorization, service identity, mTLS and managed key rotation.
- PCI DSS controls, secure card-data handling, KYC/AML integrations or regulatory reporting.
- Payment-rail adapters, reconciliation, dispute handling and real settlement finality.
- Dead-letter queues, replay operations, broker observability and cross-region event recovery.
- Rate limiting, fraud controls, abuse detection and formal threat-model review.
- Production secrets manager, encrypted database volumes, backup/restore and disaster recovery.
- Load, latency, availability or cost benchmarks.

These omissions are intentional. The repository demonstrates software design and verification around synthetic payment lifecycles; it is not represented as a production financial service.
