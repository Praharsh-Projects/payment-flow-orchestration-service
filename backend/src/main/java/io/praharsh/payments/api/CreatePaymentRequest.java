package io.praharsh.payments.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{2,63}$")
        String merchantReference,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,

        @NotBlank
        @Pattern(regexp = "^acct_[A-Za-z0-9_-]{6,48}$")
        String debtorAccountToken,

        @NotBlank
        @Pattern(regexp = "^acct_[A-Za-z0-9_-]{6,48}$")
        String creditorAccountToken
) {
}
