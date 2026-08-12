package com.assetledger.core;

import com.assetledger.domain.Asset;
import com.assetledger.domain.AssetType;
import com.assetledger.domain.Token;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Registry {
    private final Map<String, Token> tokens = new LinkedHashMap<>();
    private final Map<String, String> currentOwners = new LinkedHashMap<>();

    public Token registerAsset(
            AssetType type,
            BigDecimal value,
            String owner,
            Map<String, String> metadata
    ) {
        String assetId = "AST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String tokenId = "TOK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return registerAsset(
                assetId,
                tokenId,
                type,
                value,
                owner,
                metadata,
                Instant.now(),
                owner
        );
    }

    public Token registerAsset(
            String assetId,
            String tokenId,
            AssetType type,
            BigDecimal value,
            String originalOwner,
            Map<String, String> metadata,
            Instant createdAt,
            String currentOwner
    ) {
        if (tokens.containsKey(tokenId)) {
            throw new IllegalArgumentException("Token already exists: " + tokenId);
        }
        Token token = new Token(
                tokenId,
                new Asset(assetId, type, value, originalOwner, metadata, createdAt),
                createdAt
        );
        tokens.put(tokenId, token);
        currentOwners.put(tokenId, requireOwner(currentOwner));
        return token;
    }

    public Token getToken(String tokenId) {
        return tokens.get(tokenId);
    }

    public String currentOwner(String tokenId) {
        ensureToken(tokenId);
        return currentOwners.get(tokenId);
    }

    public void transferOwnership(String tokenId, String newOwner) {
        ensureToken(tokenId);
        currentOwners.put(tokenId, requireOwner(newOwner));
    }

    public List<Token> tokens() {
        return Collections.unmodifiableList(new ArrayList<>(tokens.values()));
    }

    public List<Token> findByOwner(String owner) {
        return tokens.values().stream()
                .filter(token -> currentOwners.get(token.tokenId()).equalsIgnoreCase(owner))
                .toList();
    }

    public int size() {
        return tokens.size();
    }

    public Map<String, String> currentOwners() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(currentOwners));
    }

    private void ensureToken(String tokenId) {
        if (!tokens.containsKey(tokenId)) {
            throw new IllegalArgumentException("Unknown token: " + tokenId);
        }
    }

    private static String requireOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner is required");
        }
        return owner.trim();
    }
}