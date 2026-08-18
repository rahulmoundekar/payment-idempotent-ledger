package com.rahul.idempotent.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IdempotentPaymentLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdempotentPaymentLedgerApplication.class, args);
    }

}
