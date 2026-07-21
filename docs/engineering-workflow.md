# Engineering workflow

AI-assisted coding was used for bounded implementation tasks such as initial file scaffolding, test-case generation and alternative design review. The workflow retained human-visible controls:

1. Lock the target requirements and unsupported claims before editing.
2. Prefer an explicit state model and data boundary over a generic CRUD surface.
3. Compile the backend before adding tests.
4. Exercise API behavior through a real random-port Spring Boot application and database migration.
5. Test idempotency, invalid transitions, stale versions, token validation, API authentication and Kafka publishing behavior.
6. Run frontend linting, strict TypeScript checks, component tests, coverage and a production build.
7. Audit dependencies and pin the patched PostCSS version when the first audit identified a moderate advisory.
8. Review architecture, limitations and CV wording against the implemented code.

AI output was treated as untrusted until it compiled and passed the relevant checks. No productivity percentage, autonomous deployment or production impact is claimed.
