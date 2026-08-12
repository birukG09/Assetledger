package com.assetledger.core;

public final class ValidationService {
    private ValidationService() {
    }

    public static void validateTransfer(
            Registry registry,
            String tokenId,
            String fromOwner,
            String toOwner
    ) {
        if (registry.getToken(tokenId) == null) {
            throw new IllegalArgumentException("Cannot transfer an unknown token");
        }
        if (fromOwner == null || fromOwner.isBlank()) {
            throw new IllegalArgumentException("Current owner is required");
        }
        if (toOwner == null || toOwner.isBlank()) {
            throw new IllegalArgumentException("Recipient is required");
        }
        String actualOwner = registry.currentOwner(tokenId);
        if (!actualOwner.equalsIgnoreCase(fromOwner.trim())) {
            throw new IllegalArgumentException("Ownership verification failed for " + tokenId);
        }
        if (actualOwner.equalsIgnoreCase(toOwner.trim())) {
            throw new IllegalArgumentException("A token cannot be transferred to its current owner");
        }
    }
}