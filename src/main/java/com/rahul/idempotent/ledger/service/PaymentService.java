package com.rahul.idempotent.ledger.service;

import com.rahul.idempotent.ledger.dto.*;
import org.springframework.data.domain.Page;

public interface PaymentService {

    PaymentResponse createPayment(
            PaymentRequest request,
            String idempotencyKey
    );

    PaymentDetailsResponse getPayment(
            String transactionReference
    );

    AccountBalanceResponse getAccountBalance(
            String accountNumber
    );

    Page<AccountTransactionResponse> getAccountTransactions(
            String accountNumber,
            int page,
            int size
    );

    PaymentConsistencyResponse checkPaymentConsistency(
            String transactionReference
    );
}