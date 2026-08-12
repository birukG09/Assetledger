package com.assetledger.core;

import com.assetledger.domain.Transaction;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Ledger {
    private final List<Transaction> transactions = new ArrayList<>();
    private final Clock clock;

    public Ledger() {
        this(Clock.systemUTC());
    }

    public Ledger(Clock clock) {
        this.clock = clock;
    }

    public synchronized Transaction recordTransfer(String fromOwner, String toOwner, String tokenId) {
        Transaction transaction = Transaction.create(
                fromOwner,
                toOwner,
                tokenId,
                Instant.now(clock),
                latestHash()
        );
        append(transaction);
        return transaction;
    }

    public synchronized void append(Transaction transaction) {
        String expectedPreviousHash = latestHash();
        if (!transaction.isValidAgainst(expectedPreviousHash)) {
            throw new IllegalArgumentException("Transaction does not link to the current ledger or has been altered");
        }
        transactions.add(transaction);
    }

    public synchronized boolean validateIntegrity() {
        String expectedPreviousHash = Transaction.GENESIS_HASH;
        for (Transaction transaction : transactions) {
            if (!transaction.isValidAgainst(expectedPreviousHash)) {
                return false;
            }
            expectedPreviousHash = transaction.hash();
        }
        return true;
    }

    public synchronized String latestHash() {
        return transactions.isEmpty()
                ? Transaction.GENESIS_HASH
                : transactions.get(transactions.size() - 1).hash();
    }

    public synchronized List<Transaction> transactions() {
        return Collections.unmodifiableList(new ArrayList<>(transactions));
    }

    public synchronized int size() {
        return transactions.size();
    }
}