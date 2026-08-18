package com.rahul.idempotent.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentDetailsResponse(

        String transactionReference,

        String status,

        BigDecimal amount,

        String currency,

        LocalDateTime createdAt,

        List<LedgerEntryResponse> entries
) {
}