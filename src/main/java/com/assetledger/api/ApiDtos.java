package com.assetledger.api;

import com.assetledger.domain.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

record CreateAssetRequest(
        @NotNull AssetType type,
        @NotNull @DecimalMin(value = "0.00") BigDecimal value,
        @NotBlank String owner,
        Map<String, String> metadata
) {
    // Compact constructor: guards against NPEs downstream if metadata is omitted from the request body
    CreateAssetRequest {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

record TransferRequest(@NotBlank String recipient) { }

record AssetResponse(
        String tokenId,
        String assetId,
        AssetType type,
        BigDecimal value,
        String originalOwner,
        String currentOwner,
        Map<String, String> metadata,
        Instant createdAt
) { }

record TransactionResponse(
        String transactionId,
        String fromOwner,
        String toOwner,
        String tokenId,
        Instant timestamp,
        String previousHash,
        String hash
) { }

record SummaryResponse(
        int assetCount,
        BigDecimal totalValue,
        int transactionCount,
        int ownerCount,
        boolean ledgerIntegrity
) { }

record HealthResponse(String status, boolean ledgerIntegrity) { }

record ErrorResponse(String error, Instant timestamp, int status) {
    // Convenience factory so callers don't repeat Instant.now() everywhere
    static ErrorResponse of(String error, int status) {
        return new ErrorResponse(error, Instant.now(), status);
    }
}
