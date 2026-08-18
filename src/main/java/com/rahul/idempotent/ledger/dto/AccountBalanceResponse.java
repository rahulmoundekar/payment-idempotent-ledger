package com.rahul.idempotent.ledger.dto;

import java.math.BigDecimal;

public record AccountBalanceResponse(
        String accountNumber,
        String currency,
        BigDecimal balance,
        String status
) {
}
