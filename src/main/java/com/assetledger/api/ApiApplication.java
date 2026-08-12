package com.assetledger.api;

import com.assetledger.service.AssetLedgerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

    @Bean(destroyMethod = "close")
    AssetLedgerService assetLedgerService(
            @Value("${assetledger.database-path:}") String configuredDatabasePath,
            @Value("${assetledger.seed-demo-data:true}") boolean seedDemoData
    ) throws SQLException, IOException {
        Path databasePath = resolveDatabasePath(configuredDatabasePath);
        Files.createDirectories(databasePath.getParent());

        AssetLedgerService service = AssetLedgerService.open(databasePath);
        if (seedDemoData) {
            service.seedDemoData();
        }
        return service;
    }

    private Path resolveDatabasePath(String configuredDatabasePath) {
        if (!configuredDatabasePath.isBlank()) {
            return Path.of(configuredDatabasePath);
        }
        return Path.of(System.getProperty("user.home"), ".assetledger", "assetledger-api.db");
    }
}
