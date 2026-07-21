package io.praharsh.payments.service;

import io.praharsh.payments.api.CreatePaymentRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class PaymentFingerprint {

    public String calculate(CreatePaymentRequest request) {
        String canonical = String.join(
                "|",
                request.merchantReference().trim(),
                request.amount().stripTrailingZeros().toPlainString(),
                request.currency().trim().toUpperCase(),
                request.debtorAccountToken().trim(),
                request.creditorAccountToken().trim()
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime.", exception);
        }
    }
}
