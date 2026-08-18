package com.rahul.idempotent.ledger.controller;

import com.rahul.idempotent.ledger.dto.*;
import com.rahul.idempotent.ledger.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        PaymentResponse response = paymentService.createPayment(request, idempotencyKey);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionReference}")
    public ResponseEntity<PaymentDetailsResponse> getPayment(@PathVariable String transactionReference) {

        PaymentDetailsResponse response = paymentService.getPayment(transactionReference);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountNumber}/balance")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(@PathVariable String accountNumber) {

        AccountBalanceResponse response = paymentService.getAccountBalance(accountNumber);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionReference}/consistency")
    public ResponseEntity<PaymentConsistencyResponse> checkConsistency(@PathVariable String transactionReference) {

        return ResponseEntity.ok(paymentService.checkPaymentConsistency(transactionReference));
    }
}