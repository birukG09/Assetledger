package com.assetledger.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

public final class Transaction {
    public static final String GENESIS_HASH = "GENESIS";

    private final String transactionId;
    private final String fromOwner;
    private final String toOwner;
    private final String tokenId;
    private final Instant timestamp;
    private final String previousHash;
    private final String hash;

    private Transaction(
            String transactionId,
            String fromOwner,
            String toOwner,
            String tokenId,
            Instant timestamp,
            String previousHash,
            String hash
    ) {
        this.transactionId = requireText(transactionId, "Transaction id");
        this.fromOwner = requireText(fromOwner, "From owner");
        this.toOwner = requireText(toOwner, "To owner");
        this.tokenId = requireText(tokenId, "Token id");
        this.timestamp = Objects.requireNonNull(timestamp, "Transaction timestamp is required");
        this.previousHash = requireText(previousHash, "Previous hash");
        this.hash = requireText(hash, "Transaction hash");
    }

    public static Transaction create(
            String fromOwner,
            String toOwner,
            String tokenId,
            Instant timestamp,
            String previousHash
    ) {
        String id = UUID.randomUUID().toString();
        String hash = calculateHash(id, fromOwner, toOwner, tokenId, timestamp, previousHash);
        return new Transaction(id, fromOwner, toOwner, tokenId, timestamp, previousHash, hash);
    }

    public static Transaction restore(
            String transactionId,
            String fromOwner,
            String toOwner,
            String tokenId,
            Instant timestamp,
            String previousHash,
            String hash
    ) {
        return new Transaction(transactionId, fromOwner, toOwner, tokenId, timestamp, previousHash, hash);
    }

    public boolean isValidAgainst(String expectedPreviousHash) {
        return previousHash.equals(expectedPreviousHash)
                && hash.equals(calculateHash(transactionId, fromOwner, toOwner, tokenId, timestamp, previousHash));
    }

    public String transactionId() {
        return transactionId;
    }

    public String fromOwner() {
        return fromOwner;
    }

    public String toOwner() {
        return toOwner;
    }

    public String tokenId() {
        return tokenId;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public String previousHash() {
        return previousHash;
    }

    public String hash() {
        return hash;
    }

    private static String calculateHash(
            String transactionId,
            String fromOwner,
            String toOwner,
            String tokenId,
            Instant timestamp,
            String previousHash
    ) {
        String payload = String.join(
                "|",
                transactionId,
                fromOwner,
                toOwner,
                tokenId,
                timestamp.toString(),
                previousHash
        );
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JDK", exception);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}