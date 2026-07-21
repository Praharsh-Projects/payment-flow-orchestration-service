package io.praharsh.payments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApiIntegrationTest {

    private static final String API_KEY = "test-api-key";
    private static final String PAYMENT_BODY = """
            {
              "merchantReference":"order-2026-001",
              "amount":1250.50,
              "currency":"SEK",
              "debtorAccountToken":"acct_debtor_01",
              "creditorAccountToken":"acct_creditor_01"
            }
            """;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM payment_transitions");
        jdbcTemplate.update("DELETE FROM payments");
    }

    @Test
    void rejectsRequestsWithoutApiKey() throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/payments", null, null, null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("unauthorized", "X-API-Key");
    }

    @Test
    void createsPaymentWithTimelineAndAllowedActions() throws Exception {
        HttpResponse<String> response = create("idem-create-0001", PAYMENT_BODY);
        JsonNode payment = json(response);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Idempotency-Replayed")).contains("false");
        assertThat(payment.get("merchantReference").stringValue()).isEqualTo("order-2026-001");
        assertThat(payment.get("status").stringValue()).isEqualTo("RECEIVED");
        assertThat(payment.get("version").asLong()).isZero();
        assertThat(payment.get("allowedTransitions").toString())
                .contains("AUTHORIZED", "REJECTED", "CANCELLED");
        assertThat(payment.get("timeline").size()).isEqualTo(1);
    }

    @Test
    void replaysIdenticalIdempotentRequest() throws Exception {
        HttpResponse<String> first = create("idem-replay-0001", PAYMENT_BODY);
        HttpResponse<String> second = create("idem-replay-0001", PAYMENT_BODY);

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(second.headers().firstValue("Idempotency-Replayed")).contains("true");
        assertThat(json(second).get("id").stringValue()).isEqualTo(json(first).get("id").stringValue());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payments", Integer.class)).isEqualTo(1);
    }

    @Test
    void rejectsIdempotencyKeyReuseForDifferentRequest() throws Exception {
        create("idem-conflict-01", PAYMENT_BODY);
        String changedAmount = PAYMENT_BODY.replace("1250.50", "1251.50");

        HttpResponse<String> response = create("idem-conflict-01", changedAmount);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("conflict", "different payment request");
    }

    @Test
    void rejectsRawAccountReferenceInsteadOfToken() throws Exception {
        String unsafe = PAYMENT_BODY.replace("acct_debtor_01", "SE3550000000054910000003");

        HttpResponse<String> response = create("idem-validation-01", unsafe);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("validation_error", "debtorAccountToken");
    }

    @Test
    void authorizesAndSettlesPaymentWithVersionChecks() throws Exception {
        String paymentId = json(create("idem-lifecycle-01", PAYMENT_BODY)).get("id").stringValue();

        HttpResponse<String> authorized = transition(paymentId, "AUTHORIZED", 0, null);
        HttpResponse<String> settled = transition(paymentId, "SETTLED", 1, null);

        assertThat(authorized.statusCode()).isEqualTo(200);
        assertThat(json(authorized).get("version").asLong()).isEqualTo(1);
        assertThat(json(authorized).get("status").stringValue()).isEqualTo("AUTHORIZED");
        assertThat(settled.statusCode()).isEqualTo(200);
        assertThat(json(settled).get("status").stringValue()).isEqualTo("SETTLED");
        assertThat(json(settled).get("allowedTransitions").isEmpty()).isTrue();
        assertThat(json(settled).get("timeline").size()).isEqualTo(3);
    }

    @Test
    void rejectsInvalidLifecycleJump() throws Exception {
        String paymentId = json(create("idem-invalid-001", PAYMENT_BODY)).get("id").stringValue();

        HttpResponse<String> response = transition(paymentId, "SETTLED", 0, null);

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("invalid_transition", "RECEIVED to SETTLED");
    }

    @Test
    void rejectsStaleTransitionVersion() throws Exception {
        String paymentId = json(create("idem-stale-0001", PAYMENT_BODY)).get("id").stringValue();

        HttpResponse<String> response = transition(paymentId, "AUTHORIZED", 7, null);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("expected version 7", "current version 0", "Refresh");
    }

    @Test
    void requiresReasonForRejectedPayment() throws Exception {
        String paymentId = json(create("idem-reason-0001", PAYMENT_BODY)).get("id").stringValue();

        HttpResponse<String> missingReason = transition(paymentId, "REJECTED", 0, null);
        HttpResponse<String> rejected = transition(paymentId, "REJECTED", 0, "Sanctions screening review");

        assertThat(missingReason.statusCode()).isEqualTo(422);
        assertThat(missingReason.body()).contains("reason is required");
        assertThat(rejected.statusCode()).isEqualTo(200);
        assertThat(json(rejected).get("statusReason").stringValue()).isEqualTo("Sanctions screening review");
    }

    @Test
    void filtersPaymentQueueByStatus() throws Exception {
        create("idem-list-000001", PAYMENT_BODY);
        String secondBody = PAYMENT_BODY.replace("order-2026-001", "order-2026-002");
        String secondId = json(create("idem-list-000002", secondBody)).get("id").stringValue();
        transition(secondId, "AUTHORIZED", 0, null);

        HttpResponse<String> response = send(
                "GET",
                "/api/v1/payments?status=AUTHORIZED",
                null,
                null,
                API_KEY
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response).size()).isEqualTo(1);
        assertThat(json(response).get(0).get("merchantReference").stringValue()).isEqualTo("order-2026-002");
    }

    @Test
    void returnsNotFoundForUnknownPayment() throws Exception {
        HttpResponse<String> response = send(
                "GET",
                "/api/v1/payments/" + UUID.randomUUID(),
                null,
                null,
                API_KEY
        );

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("not_found", "was not found");
    }

    private HttpResponse<String> create(String idempotencyKey, String body) throws Exception {
        return send("POST", "/api/v1/payments", body, idempotencyKey, API_KEY);
    }

    private HttpResponse<String> transition(
            String paymentId,
            String targetStatus,
            long expectedVersion,
            String reason
    ) throws Exception {
        String reasonJson = reason == null ? "null" : "\"" + reason + "\"";
        String body = """
                {"targetStatus":"%s","expectedVersion":%d,"reason":%s}
                """.formatted(targetStatus, expectedVersion, reasonJson);
        return send(
                "POST",
                "/api/v1/payments/" + paymentId + "/transitions",
                body,
                null,
                API_KEY
        );
    }

    private HttpResponse<String> send(
            String method,
            String path,
            String body,
            String idempotencyKey,
            String apiKey
    ) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (apiKey != null) {
            builder.header("X-API-Key", apiKey);
        }
        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }
}
