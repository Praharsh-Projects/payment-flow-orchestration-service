package io.praharsh.payments;

import io.praharsh.payments.api.CreatePaymentRequest;
import io.praharsh.payments.service.PaymentFingerprint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFingerprintTest {

    private final PaymentFingerprint fingerprint = new PaymentFingerprint();

    @Test
    void normalizesEquivalentAmountAndCurrency() {
        CreatePaymentRequest first = request(new BigDecimal("10.00"), "SEK");
        CreatePaymentRequest second = request(new BigDecimal("10.0"), "SEK");

        assertThat(fingerprint.calculate(first)).isEqualTo(fingerprint.calculate(second));
    }

    @Test
    void changesWhenFinancialValueChanges() {
        assertThat(fingerprint.calculate(request(new BigDecimal("10.00"), "SEK")))
                .isNotEqualTo(fingerprint.calculate(request(new BigDecimal("11.00"), "SEK")));
    }

    private static CreatePaymentRequest request(BigDecimal amount, String currency) {
        return new CreatePaymentRequest(
                "order-100",
                amount,
                currency,
                "acct_debtor_01",
                "acct_creditor_01"
        );
    }
}
