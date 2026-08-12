package com.assetledger.ui;

import com.assetledger.service.AssetLedgerService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;

public final class AssetLedgerApp extends Application {
    private AssetLedgerService service;

    @Override
    public void start(Stage stage) throws Exception {
        Path databasePath = Path.of(
                System.getProperty("user.home"),
                ".assetledger",
                "assetledger.db"
        );
        service = AssetLedgerService.open(databasePath);
        service.seedDemoData();

        DashboardController dashboard = new DashboardController(service);
        Scene scene = new Scene(dashboard, 1280, 820);
        scene.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm()
        );

        stage.setTitle("AssetLedger");
        stage.setMinWidth(1060);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (service != null) {
            service.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}