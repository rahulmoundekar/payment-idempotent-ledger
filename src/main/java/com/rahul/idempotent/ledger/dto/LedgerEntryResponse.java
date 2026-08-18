package com.rahul.idempotent.ledger.dto;

import java.math.BigDecimal;

public record LedgerEntryResponse(

        String accountNumber,

        String entryType,

        BigDecimal amount
) {
}