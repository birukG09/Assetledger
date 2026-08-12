package com.assetledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class Asset {
    private final String id;
    private final AssetType type;
    private final BigDecimal value;
    private final String owner;
    private final Map<String, String> metadata;
    private final Instant registeredAt;

    public Asset(
            String id,
            AssetType type,
            BigDecimal value,
            String owner,
            Map<String, String> metadata,
            Instant registeredAt
    ) {
        this.id = requireText(id, "Asset id");
        this.type = Objects.requireNonNull(type, "Asset type is required");
        this.value = Objects.requireNonNull(value, "Asset value is required");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Asset value cannot be negative");
        }
        this.owner = requireText(owner, "Asset owner");
        this.metadata = Map.copyOf(Objects.requireNonNull(metadata, "Asset metadata is required"));
        this.registeredAt = Objects.requireNonNull(registeredAt, "Registration time is required");
    }

    public String id() {
        return id;
    }

    public AssetType type() {
        return type;
    }

    public BigDecimal value() {
        return value;
    }

    public String owner() {
        return owner;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}