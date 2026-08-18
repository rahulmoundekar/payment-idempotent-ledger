package com.rahul.idempotent.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountTransactionResponse(

        String transactionReference,

        String entryType,

        BigDecimal amount,

        String currency,

        String status,

        LocalDateTime createdAt
) {
}