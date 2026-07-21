# API contract

Every `/api/v1/**` request requires `X-API-Key`. Payment creation also requires an `Idempotency-Key` between 8 and 80 characters.

## Endpoints

| Method | Path | Behavior |
| --- | --- | --- |
| `POST` | `/api/v1/payments` | Create or replay an idempotent synthetic payment instruction |
| `GET` | `/api/v1/payments` | List payments, optionally filtered by `status` |
| `GET` | `/api/v1/payments/{id}` | Return current state, allowed transitions and timeline |
| `POST` | `/api/v1/payments/{id}/transitions` | Apply a validated transition using the visible version |
| `GET` | `/api/v1/events/stream` | Server-sent payment status events |
| `GET` | `/actuator/health` | Service health; API key is not required |

## State model

```text
RECEIVED -> AUTHORIZED -> SETTLED
    |           |
    +-> REJECTED|
    +-> CANCELLED <-+
```

`REJECTED`, `CANCELLED` and `SETTLED` are terminal. Rejection and cancellation require an operator reason.

## Conflict behavior

- Reusing an idempotency key with a different request returns `409` and leaves the original payment unchanged.
- Sending an old `expectedVersion` returns `409` with the current version in the message.
- Requesting a state jump that is not in the state model returns `422`.
- Schema or token-format failures return `400` with field details.

## Token boundary

Account fields accept only `acct_` references followed by 6 to 48 safe identifier characters. The API does not accept card numbers, IBANs, credentials or free-form financial identifiers.
