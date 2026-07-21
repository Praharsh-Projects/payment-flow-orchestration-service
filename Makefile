.PHONY: backend-test frontend-install frontend-test frontend-build quality

backend-test:
	cd backend && ./gradlew clean test jacocoTestReport --no-daemon

frontend-install:
	cd frontend && npm ci

frontend-test:
	cd frontend && npm run lint && npm run typecheck && npm run test:coverage && npm audit --audit-level=moderate

frontend-build:
	cd frontend && npm run build

quality: backend-test frontend-test frontend-build
