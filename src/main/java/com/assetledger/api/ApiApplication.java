package com.assetledger.api;

import com.assetledger.service.AssetLedgerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;
import java.sql.SQLException;

@SpringBootApplication
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

    @Bean(destroyMethod = "close")
    AssetLedgerService assetLedgerService(
            @Value("${assetledger.database-path:}") String configuredDatabasePath
    ) throws SQLException {
        Path databasePath = configuredDatabasePath.isBlank()
                ? Path.of(System.getProperty("user.home"), ".assetledger", "assetledger-api.db")
                : Path.of(configuredDatabasePath);
        AssetLedgerService service = AssetLedgerService.open(databasePath);
        service.seedDemoData();
        return service;
    }
}