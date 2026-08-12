package com.assetledger.service;

import com.assetledger.core.Ledger;
import com.assetledger.core.Registry;
import com.assetledger.core.ValidationService;
import com.assetledger.domain.AssetType;
import com.assetledger.domain.Token;
import com.assetledger.domain.Transaction;
import com.assetledger.persistence.SqliteAssetLedgerStore;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public final class AssetLedgerService implements AutoCloseable {
    private final Registry registry;
    private final Ledger ledger;
    private final SqliteAssetLedgerStore store;

    private AssetLedgerService(
            Registry registry,
            Ledger ledger,
            SqliteAssetLedgerStore store
    ) {
        this.registry = registry;
        this.ledger = ledger;
        this.store = store;
    }

    public static AssetLedgerService open(Path databasePath) throws SQLException {
        SqliteAssetLedgerStore store = new SqliteAssetLedgerStore(databasePath);
        Registry registry = new Registry();
        for (SqliteAssetLedgerStore.PersistedToken token : store.loadTokens()) {
            registry.registerAsset(
                    token.assetId(),
                    token.tokenId(),
                    token.type(),
                    token.value(),
                    token.originalOwner(),
                    token.metadata(),
                    token.createdAt(),
                    token.currentOwner()
            );
        }
        Ledger ledger = new Ledger();
        for (Transaction transaction : store.loadTransactions()) {
            ledger.append(transaction);
        }
        if (!ledger.validateIntegrity()) {
            store.close();
            throw new SQLException("Stored ledger failed integrity validation");
        }
        return new AssetLedgerService(registry, ledger, store);
    }

    public Token registerAsset(
            AssetType type,
            BigDecimal value,
            String owner,
            Map<String, String> metadata
    ) throws SQLException {
        Token token = registry.registerAsset(type, value, owner, metadata);
        store.saveToken(token, registry.currentOwner(token.tokenId()));
        return token;
    }

    public Transaction transfer(String tokenId, String recipient) throws SQLException {
        String currentOwner = registry.currentOwner(tokenId);
        ValidationService.validateTransfer(registry, tokenId, currentOwner, recipient);
        Transaction transaction = ledger.recordTransfer(currentOwner, recipient.trim(), tokenId);
        registry.transferOwnership(tokenId, recipient);
        store.updateCurrentOwner(tokenId, recipient.trim());
        store.saveTransaction(transaction);
        return transaction;
    }

    public void seedDemoData() throws SQLException {
        if (registry.size() > 0 || ledger.size() > 0) {
            return;
        }

        Token compressor = registerAsset(
                AssetType.EQUIPMENT,
                new BigDecimal("48200.00"),
                "Northstar Manufacturing",
                Map.of("location", "Plant 04", "serial", "CMP-2048")
        );
        Token invoice = registerAsset(
                AssetType.INVOICE,
                new BigDecimal("18750.00"),
                "Northstar Manufacturing",
                Map.of("customer", "Cedar & Co.", "due", "2026-09-14")
        );
        Token inventory = registerAsset(
                AssetType.INVENTORY,
                new BigDecimal("9240.00"),
                "Northstar Manufacturing",
                Map.of("sku", "BRG-440", "warehouse", "West Bay")
        );
        Token tooling = registerAsset(
                AssetType.EQUIPMENT,
                new BigDecimal("31600.00"),
                "Northstar Manufacturing",
                Map.of("location", "Tool Room", "serial", "TLG-7731")
        );

        transfer(compressor.tokenId(), "Arcadia Capital");
        transfer(compressor.tokenId(), "Beacon Works");
        transfer(compressor.tokenId(), "Northstar Manufacturing");
        transfer(invoice.tokenId(), "Cedar & Co.");
        transfer(invoice.tokenId(), "Northstar Manufacturing");
        transfer(inventory.tokenId(), "Arcadia Capital");
        transfer(inventory.tokenId(), "Northstar Manufacturing");
        transfer(tooling.tokenId(), "Cedar & Co.");
        transfer(tooling.tokenId(), "Meridian Holdings");
        transfer(tooling.tokenId(), "Northstar Manufacturing");
    }

    public Registry registry() {
        return registry;
    }

    public Ledger ledger() {
        return ledger;
    }

    public List<Token> tokens() {
        return registry.tokens();
    }

    public List<Transaction> transactions() {
        return ledger.transactions();
    }

    @Override
    public void close() throws SQLException {
        store.close();
    }
}