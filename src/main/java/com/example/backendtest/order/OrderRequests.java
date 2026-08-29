package com.example.backendtest.order;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class OrderRequests {

    private OrderRequests() {
    }

    public record Create(
            @NotNull @DecimalMin("0.01") @DecimalMax("1000000.00") BigDecimal amount,
            @NotNull @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO code") String currency,
            @Size(max = 500) String comment) {
    }

    /** PATCH: only a NEW order's amount, currency and comment may change. */
    public record Patch(
            @DecimalMin("0.01") @DecimalMax("1000000.00") BigDecimal amount,
            @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO code") String currency,
            @Size(max = 500) String comment) {
    }
}
