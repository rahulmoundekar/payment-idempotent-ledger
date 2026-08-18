package com.rahul.idempotent.ledger.controller;

import com.rahul.idempotent.ledger.dto.AccountBalanceResponse;
import com.rahul.idempotent.ledger.dto.AccountTransactionResponse;
import com.rahul.idempotent.ledger.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final PaymentService paymentService;

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(@PathVariable String accountNumber) {

        AccountBalanceResponse response = paymentService.getAccountBalance(accountNumber);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<Page<AccountTransactionResponse>> getTransactions(@PathVariable String accountNumber,

                                                                            @RequestParam(defaultValue = "0") int page,

                                                                            @RequestParam(defaultValue = "10") int size) {

        Page<AccountTransactionResponse> response = paymentService.getAccountTransactions(accountNumber, page, size);

        return ResponseEntity.ok(response);
    }
}