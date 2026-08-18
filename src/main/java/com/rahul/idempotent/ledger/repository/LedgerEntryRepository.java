package com.rahul.idempotent.ledger.repository;

import com.rahul.idempotent.ledger.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransactionId(Long transactionId);

    Page<LedgerEntry> findByAccountId(
            Long accountId,
            Pageable pageable
    );
}