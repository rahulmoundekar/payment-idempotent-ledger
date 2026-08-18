package com.rahul.idempotent.ledger.dto;

import java.math.BigDecimal;

public record PaymentConsistencyResponse(

        String transactionReference,

        BigDecimal debitTotal,

        BigDecimal creditTotal,

        boolean balanced,

        String status
) {
}