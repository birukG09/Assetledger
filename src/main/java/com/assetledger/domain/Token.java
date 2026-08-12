package com.assetledger.domain;

import java.time.Instant;
import java.util.Objects;

public record Token(String tokenId, Asset asset, Instant createdAt) {
    public Token {
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("Token id is required");
        }
        Objects.requireNonNull(asset, "Token asset is required");
        Objects.requireNonNull(createdAt, "Token creation time is required");
    }
}