package com.rahul.idempotent.ledger.repository;

import com.rahul.idempotent.ledger.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository
        extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByIdempotencyKey(
            String idempotencyKey
    );
}