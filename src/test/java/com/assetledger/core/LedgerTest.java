package com.assetledger.core;

import com.assetledger.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerTest {
    @Test
    void appendsHashLinkedTransactionsAndValidatesTheChain() {
        Ledger ledger = new Ledger();
        ledger.recordTransfer("Northstar Manufacturing", "Arcadia Capital", "TOK-001");
        ledger.recordTransfer("Arcadia Capital", "Northstar Manufacturing", "TOK-001");

        assertTrue(ledger.validateIntegrity());
        assertTrue(ledger.transactions().get(1).previousHash()
                .equals(ledger.transactions().get(0).hash()));
    }

    @Test
    void rejectsATransactionWithTheWrongPreviousHash() {
        Ledger ledger = new Ledger();
        Transaction transaction = Transaction.create(
                "Northstar Manufacturing",
                "Arcadia Capital",
                "TOK-001",
                Instant.parse("2026-01-01T00:00:00Z"),
                "not-the-genesis-hash"
        );

        assertThrows(IllegalArgumentException.class, () -> ledger.append(transaction));
    }

    @Test
    void rejectsTamperingWhenTheStoredHashNoLongerMatches() {
        Ledger ledger = new Ledger();
        Transaction original = ledger.recordTransfer("A", "B", "TOK-001");
        Transaction tampered = Transaction.restore(
                original.transactionId(),
                original.fromOwner(),
                "C",
                original.tokenId(),
                original.timestamp(),
                original.previousHash(),
                original.hash()
        );

        Ledger tamperedLedger = new Ledger();
        assertDoesNotThrow(() -> tamperedLedger.append(original));
        assertThrows(IllegalArgumentException.class, () -> tamperedLedger.append(tampered));
        assertFalse(tampered.isValidAgainst(Transaction.GENESIS_HASH));
    }
}