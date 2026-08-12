package com.assetledger.persistence;

import com.assetledger.domain.AssetType;
import com.assetledger.domain.Token;
import com.assetledger.domain.Transaction;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SqliteAssetLedgerStore implements AutoCloseable {
    private final Connection connection;

    public SqliteAssetLedgerStore(Path databasePath) throws SQLException {
        try {
            Path parent = databasePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception exception) {
            throw new SQLException("Could not prepare the SQLite database folder", exception);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
        initializeSchema();
    }

    public void saveToken(Token token, String currentOwner) throws SQLException {
        String sql = """
                INSERT OR REPLACE INTO tokens
                    (token_id, asset_id, asset_type, asset_value, original_owner, current_owner, metadata, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token.tokenId());
            statement.setString(2, token.asset().id());
            statement.setString(3, token.asset().type().name());
            statement.setString(4, token.asset().value().toPlainString());
            statement.setString(5, token.asset().owner());
            statement.setString(6, currentOwner);
            statement.setString(7, MetadataCodec.encode(token.asset().metadata()));
            statement.setString(8, token.createdAt().toString());
            statement.executeUpdate();
        }
    }

    public void updateCurrentOwner(String tokenId, String currentOwner) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tokens SET current_owner = ? WHERE token_id = ?"
        )) {
            statement.setString(1, currentOwner);
            statement.setString(2, tokenId);
            statement.executeUpdate();
        }
    }

    public void saveTransaction(Transaction transaction) throws SQLException {
        String sql = """
                INSERT OR REPLACE INTO transactions
                    (transaction_id, from_owner, to_owner, token_id, timestamp, previous_hash, hash)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transaction.transactionId());
            statement.setString(2, transaction.fromOwner());
            statement.setString(3, transaction.toOwner());
            statement.setString(4, transaction.tokenId());
            statement.setString(5, transaction.timestamp().toString());
            statement.setString(6, transaction.previousHash());
            statement.setString(7, transaction.hash());
            statement.executeUpdate();
        }
    }

    public List<PersistedToken> loadTokens() throws SQLException {
        List<PersistedToken> tokens = new ArrayList<>();
        String sql = """
                SELECT token_id, asset_id, asset_type, asset_value, original_owner,
                       current_owner, metadata, created_at
                FROM tokens ORDER BY created_at
                """;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                tokens.add(new PersistedToken(
                        result.getString("asset_id"),
                        result.getString("token_id"),
                        AssetType.valueOf(result.getString("asset_type")),
                        new BigDecimal(result.getString("asset_value")),
                        result.getString("original_owner"),
                        MetadataCodec.decode(result.getString("metadata")),
                        Instant.parse(result.getString("created_at")),
                        result.getString("current_owner")
                ));
            }
        }
        return tokens;
    }

    public List<Transaction> loadTransactions() throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = """
                SELECT transaction_id, from_owner, to_owner, token_id, timestamp, previous_hash, hash
                FROM transactions ORDER BY rowid
                """;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                transactions.add(Transaction.restore(
                        result.getString("transaction_id"),
                        result.getString("from_owner"),
                        result.getString("to_owner"),
                        result.getString("token_id"),
                        Instant.parse(result.getString("timestamp")),
                        result.getString("previous_hash"),
                        result.getString("hash")
                ));
            }
        }
        return transactions;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    private void initializeSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tokens (
                        token_id TEXT PRIMARY KEY,
                        asset_id TEXT NOT NULL UNIQUE,
                        asset_type TEXT NOT NULL,
                        asset_value TEXT NOT NULL,
                        original_owner TEXT NOT NULL,
                        current_owner TEXT NOT NULL,
                        metadata TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        transaction_id TEXT PRIMARY KEY,
                        from_owner TEXT NOT NULL,
                        to_owner TEXT NOT NULL,
                        token_id TEXT NOT NULL,
                        timestamp TEXT NOT NULL,
                        previous_hash TEXT NOT NULL,
                        hash TEXT NOT NULL UNIQUE
                    )
                    """);
        }
    }

    public record PersistedToken(
            String assetId,
            String tokenId,
            AssetType type,
            BigDecimal value,
            String originalOwner,
            Map<String, String> metadata,
            Instant createdAt,
            String currentOwner
    ) {
    }
}