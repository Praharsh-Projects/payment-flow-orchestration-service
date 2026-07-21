package io.praharsh.payments.service;

import io.praharsh.payments.api.PaymentView;

public record CreatePaymentResult(PaymentView payment, boolean replayed) {
}
