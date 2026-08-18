package com.rahul.idempotent.ledger.repository;

import com.rahul.idempotent.ledger.entity.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LedgerTransactionRepository
        extends JpaRepository<LedgerTransaction, Long> {

    Optional<LedgerTransaction> findByTransactionReference(
            String transactionReference
    );

    boolean existsByTransactionReference(String transactionReference);
}
