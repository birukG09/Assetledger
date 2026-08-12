package com.assetledger.core;

import com.assetledger.domain.AssetType;
import com.assetledger.domain.Token;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationServiceTest {
    @Test
    void requiresTheCurrentOwnerToAuthorizeATransfer() {
        Registry registry = new Registry();
        Token token = registry.registerAsset(
                AssetType.EQUIPMENT,
                new BigDecimal("100.00"),
                "Northstar",
                Map.of()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ValidationService.validateTransfer(
                        registry,
                        token.tokenId(),
                        "Impersonator",
                        "Arcadia"
                )
        );
    }

    @Test
    void preventsTransfersToTheSameOwner() {
        Registry registry = new Registry();
        Token token = registry.registerAsset(
                AssetType.INVOICE,
                new BigDecimal("100.00"),
                "Northstar",
                Map.of()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ValidationService.validateTransfer(
                        registry,
                        token.tokenId(),
                        "Northstar",
                        "Northstar"
                )
        );
    }
}