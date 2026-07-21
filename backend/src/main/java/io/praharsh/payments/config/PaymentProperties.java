package io.praharsh.payments.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "payments")
public class PaymentProperties {

    @NotBlank
    private String apiKey = "local-review-key";

    @NotBlank
    private String eventsTopic = "payment-status-events";

    @Min(1)
    @Max(100)
    private int outboxBatchSize = 25;

    private boolean outboxEnabled = true;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEventsTopic() {
        return eventsTopic;
    }

    public void setEventsTopic(String eventsTopic) {
        this.eventsTopic = eventsTopic;
    }

    public int getOutboxBatchSize() {
        return outboxBatchSize;
    }

    public void setOutboxBatchSize(int outboxBatchSize) {
        this.outboxBatchSize = outboxBatchSize;
    }

    public boolean isOutboxEnabled() {
        return outboxEnabled;
    }

    public void setOutboxEnabled(boolean outboxEnabled) {
        this.outboxEnabled = outboxEnabled;
    }
}
