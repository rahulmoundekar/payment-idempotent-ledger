package com.rahul.idempotent.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(

        String transactionReference,

        String status,

        BigDecimal amount,

        String currency,

        LocalDateTime createdAt
) {
}