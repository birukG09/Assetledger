package com.assetledger.service;

import com.assetledger.domain.AssetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetLedgerServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsRegistryAndLedgerAcrossServiceSessions() throws Exception {
        Path database = temporaryDirectory.resolve("asset-ledger.db");
        String tokenId;

        try (AssetLedgerService service = AssetLedgerService.open(database)) {
            tokenId = service.registerAsset(
                    AssetType.INVENTORY,
                    new BigDecimal("2400.00"),
                    "Northstar",
                    Map.of("sku", "BRG-440")
            ).tokenId();
            service.transfer(tokenId, "Arcadia");
            assertTrue(service.ledger().validateIntegrity());
        }

        try (AssetLedgerService reopened = AssetLedgerService.open(database)) {
            assertEquals("Arcadia", reopened.registry().currentOwner(tokenId));
            assertEquals(1, reopened.ledger().size());
            assertTrue(reopened.ledger().validateIntegrity());
        }
    }

    @Test
    void seedsTenDemoTransfers() throws Exception {
        try (AssetLedgerService service = AssetLedgerService.open(
                temporaryDirectory.resolve("demo.db")
        )) {
            service.seedDemoData();
            assertEquals(4, service.registry().size());
            assertEquals(10, service.ledger().size());
            assertTrue(service.ledger().validateIntegrity());
        }
    }
}