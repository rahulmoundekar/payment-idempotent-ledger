package com.rahul.idempotent.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(

        @NotBlank(message = "fromAccount is required")
        String fromAccount,

        @NotBlank(message = "toAccount is required")
        String toAccount,

        @NotNull(message = "amount is required")
        @DecimalMin(
                value = "0.01",
                message = "amount must be greater than zero"
        )
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        String currency
) {
}