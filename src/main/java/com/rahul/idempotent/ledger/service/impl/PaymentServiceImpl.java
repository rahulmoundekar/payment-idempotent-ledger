package com.rahul.idempotent.ledger.service.impl;

import com.rahul.idempotent.ledger.dto.*;
import com.rahul.idempotent.ledger.entity.*;
import com.rahul.idempotent.ledger.exception.IdempotencyConflictException;
import com.rahul.idempotent.ledger.exception.ResourceNotFoundException;
import com.rahul.idempotent.ledger.repository.AccountRepository;
import com.rahul.idempotent.ledger.repository.IdempotencyKeyRepository;
import com.rahul.idempotent.ledger.repository.LedgerEntryRepository;
import com.rahul.idempotent.ledger.repository.LedgerTransactionRepository;
import com.rahul.idempotent.ledger.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final AccountRepository accountRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PaymentResponse createPayment(PaymentRequest request, String idempotencyKey) {

        // 1. Check idempotency
        var existingKey = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);

        if (existingKey.isPresent()) {

            IdempotencyKey existing = existingKey.get();

            boolean sameRequest = existing.getFromAccount().equals(request.fromAccount()) && existing.getToAccount().equals(request.toAccount()) && existing.getAmount().compareTo(request.amount()) == 0 && existing.getCurrency().equalsIgnoreCase(request.currency());

            if (!sameRequest) {
                throw new IdempotencyConflictException("Idempotency-Key was already used for a different request");
            }

            return toResponse(existing.getTransaction());
        }

        // 2. Load accounts

        String firstAccountNumber;
        String secondAccountNumber;

        if (request.fromAccount().compareTo(request.toAccount()) < 0) {

            firstAccountNumber = request.fromAccount();
            secondAccountNumber = request.toAccount();

        } else {

            firstAccountNumber = request.toAccount();
            secondAccountNumber = request.fromAccount();
        }

        Account firstAccount = accountRepository.findWithLockByAccountNumber(firstAccountNumber).orElseThrow(() -> new IllegalArgumentException("Source account not found: " + firstAccountNumber));

        Account secondAccount = accountRepository.findWithLockByAccountNumber(secondAccountNumber).orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + secondAccountNumber));

        Account fromAccount;
        Account toAccount;

        if (firstAccount.getAccountNumber().equals(request.fromAccount())) {

            fromAccount = firstAccount;
            toAccount = secondAccount;

        } else {

            fromAccount = secondAccount;
            toAccount = firstAccount;
        }

        if (fromAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        // 3. Basic validation
        if (fromAccount.getAccountNumber().equals(toAccount.getAccountNumber())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        if (fromAccount.getStatus() != AccountStatus.ACTIVE || toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Both accounts must be active");
        }

        if (!fromAccount.getCurrency().equalsIgnoreCase(request.currency()) || !toAccount.getCurrency().equalsIgnoreCase(request.currency())) {
            throw new IllegalArgumentException("Currency mismatch");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.amount()));
        toAccount.setBalance(toAccount.getBalance().add(request.amount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // 4. Create transaction
        LedgerTransaction transaction = LedgerTransaction.builder().transactionReference(generateTransactionReference()).amount(request.amount()).currency(request.currency().toUpperCase()).status(TransactionStatus.COMPLETED).build();

        transaction = transactionRepository.save(transaction);

        // 5. Create DEBIT entry
        LedgerEntry debitEntry = LedgerEntry.builder().transaction(transaction).account(fromAccount).entryType(EntryType.DEBIT).amount(request.amount()).build();

        // 6. Create CREDIT entry
        LedgerEntry creditEntry = LedgerEntry.builder().transaction(transaction).account(toAccount).entryType(EntryType.CREDIT).amount(request.amount()).build();

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        // 7. Validate double-entry ledger
        validateLedgerBalance(List.of(debitEntry, creditEntry));

        // 8. Store idempotency key
        IdempotencyKey key = IdempotencyKey.builder().idempotencyKey(idempotencyKey).transaction(transaction).fromAccount(request.fromAccount()).toAccount(request.toAccount()).amount(request.amount()).currency(request.currency().toUpperCase()).build();

        idempotencyKeyRepository.save(key);

        // 9. Return result
        return toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailsResponse getPayment(String transactionReference) {

        LedgerTransaction transaction = transactionRepository.findByTransactionReference(transactionReference).orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + transactionReference));

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(transaction.getId());

        List<LedgerEntryResponse> entryResponses = entries.stream().map(entry -> new LedgerEntryResponse(entry.getAccount().getAccountNumber(), entry.getEntryType().name(), entry.getAmount())).toList();

        return new PaymentDetailsResponse(transaction.getTransactionReference(), transaction.getStatus().name(), transaction.getAmount(), transaction.getCurrency(), transaction.getCreatedAt(), entryResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountBalanceResponse getAccountBalance(String accountNumber) {

        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        return new AccountBalanceResponse(account.getAccountNumber(), account.getCurrency(), account.getBalance(), account.getStatus().name());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountTransactionResponse> getAccountTransactions(String accountNumber, int page, int size) {

        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transaction.createdAt"));

        Page<LedgerEntry> entries = ledgerEntryRepository.findByAccountId(account.getId(), pageable);

        return entries.map(entry -> {

            LedgerTransaction transaction = entry.getTransaction();

            return new AccountTransactionResponse(transaction.getTransactionReference(), entry.getEntryType().name(), entry.getAmount(), transaction.getCurrency(), transaction.getStatus().name(), transaction.getCreatedAt());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentConsistencyResponse checkPaymentConsistency(String transactionReference) {

        LedgerTransaction transaction = transactionRepository.findByTransactionReference(transactionReference).orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + transactionReference));

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(transaction.getId());

        BigDecimal debitTotal = entries.stream().filter(entry -> entry.getEntryType() == EntryType.DEBIT).map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditTotal = entries.stream().filter(entry -> entry.getEntryType() == EntryType.CREDIT).map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean balanced = debitTotal.compareTo(creditTotal) == 0;

        return new PaymentConsistencyResponse(transaction.getTransactionReference(), debitTotal, creditTotal, balanced, balanced ? "BALANCED" : "IMBALANCED");
    }

    private String generateTransactionReference() {
        return "TXN-" + UUID.randomUUID();
    }

    private PaymentResponse toResponse(LedgerTransaction transaction) {
        return new PaymentResponse(transaction.getTransactionReference(), transaction.getStatus().name(), transaction.getAmount(), transaction.getCurrency(), transaction.getCreatedAt());
    }

    private void validateLedgerBalance(List<LedgerEntry> entries) {

        BigDecimal debitTotal = entries.stream().filter(entry -> entry.getEntryType() == EntryType.DEBIT).map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditTotal = entries.stream().filter(entry -> entry.getEntryType() == EntryType.CREDIT).map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new IllegalStateException("Ledger imbalance detected. Debit=" + debitTotal + ", Credit=" + creditTotal);
        }
    }

}