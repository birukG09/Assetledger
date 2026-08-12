package com.assetledger.ui;

import com.assetledger.domain.AssetType;
import com.assetledger.domain.Token;
import com.assetledger.domain.Transaction;
import com.assetledger.service.AssetLedgerService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class DashboardController extends BorderPane {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern(
            "MMM d, yyyy  HH:mm",
            Locale.US
    ).withZone(ZoneId.systemDefault());

    private final AssetLedgerService service;
    private final TableView<AssetRow> assetTable = new TableView<>();
    private final TableView<TransactionRow> transactionTable = new TableView<>();
    private final TextField searchField = new TextField();
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private final ComboBox<String> transferToken = new ComboBox<>();
    private final TextField recipientField = new TextField();
    private final Label statusLabel = new Label("Ledger integrity verified");
    private final Label assetCount = new Label();
    private final Label totalValue = new Label();
    private final Label transactionCount = new Label();
    private final Label ownerCount = new Label();
    private final List<Button> navigationButtons = new ArrayList<>();
    private ScrollPane contentScroll;
    private Node registryPanel;
    private Node ledgerPanel;

    public DashboardController(AssetLedgerService service) {
        this.service = service;
        getStyleClass().add("app-shell");
        setLeft(createSidebar());
        setCenter(createMainContent());
        refresh();
    }

    private Node createSidebar() {
        VBox sidebar = new VBox(24);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(28, 20, 24, 20));
        sidebar.setPrefWidth(230);

        HBox brand = new HBox(10);
        brand.setAlignment(Pos.CENTER_LEFT);
        Label mark = new Label("AL");
        mark.getStyleClass().add("brand-mark");
        VBox brandText = new VBox(1);
        Label name = new Label("AssetLedger");
        name.getStyleClass().add("brand-name");
        Label caption = new Label("Internal ledger");
        caption.getStyleClass().add("brand-caption");
        brandText.getChildren().addAll(name, caption);
        brand.getChildren().addAll(mark, brandText);

        VBox navigation = new VBox(8);
        navigation.getChildren().addAll(
                navButton("Overview", true, () -> scrollTo(null)),
                navButton("Asset registry", false, () -> scrollTo(registryPanel)),
                navButton("Ledger history", false, () -> scrollTo(ledgerPanel))
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox system = new VBox(8);
        Label systemTitle = new Label("SYSTEM STATUS");
        systemTitle.getStyleClass().add("eyebrow");
        Label systemText = new Label("Java 17  •  SQLite");
        systemText.getStyleClass().add("sidebar-meta");
        HBox status = new HBox(8, new Label("●"), new Label("Operational"));
        status.getStyleClass().add("sidebar-status");
        system.getChildren().addAll(systemTitle, systemText, status);

        sidebar.getChildren().addAll(brand, navigation, spacer, system);
        return sidebar;
    }

    private Button navButton(String text, boolean active, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("nav-button");
        if (active) {
            button.getStyleClass().add("nav-button-active");
        }
        navigationButtons.add(button);
        button.setOnAction(event -> {
            navigationButtons.forEach(navButton -> navButton.getStyleClass().remove("nav-button-active"));
            button.getStyleClass().add("nav-button-active");
            action.run();
        });
        return button;
    }

    private Node createMainContent() {
        VBox content = new VBox(24);
        content.getStyleClass().add("content");
        content.setPadding(new Insets(30, 38, 32, 38));

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox heading = new VBox(4);
        Label eyebrow = new Label("PORTFOLIO CONTROL CENTER");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Asset operations, in one source of truth.");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Tokenized assets and ownership changes, secured by an append-only chain.");
        subtitle.getStyleClass().add("page-subtitle");
        heading.getChildren().addAll(eyebrow, title, subtitle);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button registerButton = new Button("Register asset");
        registerButton.getStyleClass().add("primary-button");
        registerButton.setOnAction(event -> showRegisterDialog());
        header.getChildren().addAll(heading, headerSpacer, registerButton);

        FlowPane summary = new FlowPane(14, 14);
        summary.getChildren().addAll(
                summaryCard("REGISTERED ASSETS", assetCount, "Tokenized in registry", "accent-blue"),
                summaryCard("PORTFOLIO VALUE", totalValue, "Current face value", "accent-gold"),
                summaryCard("LEDGER EVENTS", transactionCount, "Validated transfers", "accent-green"),
                summaryCard("CURRENT OWNERS", ownerCount, "Distinct holders", "accent-purple")
        );

        HBox registryHeading = new HBox(14);
        registryHeading.setAlignment(Pos.CENTER_LEFT);
        VBox registryTitle = new VBox(3);
        Label registryLabel = new Label("Asset registry");
        registryLabel.getStyleClass().add("section-title");
        Label registryCaption = new Label("Search the current ownership state of every token.");
        registryCaption.getStyleClass().add("section-caption");
        registryTitle.getChildren().addAll(registryLabel, registryCaption);
        Region registrySpacer = new Region();
        HBox.setHgrow(registrySpacer, Priority.ALWAYS);
        searchField.setPromptText("Search token, asset, owner...");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshAssetTable());
        typeFilter.getItems().add("All types");
        typeFilter.getItems().addAll(List.of("Inventory", "Invoice", "Equipment"));
        typeFilter.setValue("All types");
        typeFilter.getStyleClass().add("filter-box");
        typeFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshAssetTable());
        registryHeading.getChildren().addAll(registryTitle, registrySpacer, searchField, typeFilter);

        configureAssetTable();
        VBox registryBox = new VBox(16, registryHeading, assetTable);
        registryPanel = registryBox;
        registryBox.getStyleClass().add("panel");
        registryBox.setPadding(new Insets(20));
        VBox.setVgrow(assetTable, Priority.ALWAYS);

        HBox lowerPanels = new HBox(18);
        ledgerPanel = createLedgerPanel();
        lowerPanels.getChildren().addAll(createTransferPanel(), ledgerPanel);
        HBox.setHgrow(lowerPanels.getChildren().get(1), Priority.ALWAYS);

        HBox footer = new HBox(8, new Label("●"), statusLabel);
        footer.getStyleClass().add("footer-status");

        content.getChildren().addAll(header, summary, registryPanel, lowerPanels, footer);
        VBox.setVgrow(registryPanel, Priority.ALWAYS);

        contentScroll = new ScrollPane(content);
        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.getStyleClass().add("content-scroll");
        return contentScroll;
    }

    private void scrollTo(Node target) {
        if (contentScroll == null) {
            return;
        }
        if (target == null) {
            contentScroll.setVvalue(0);
            return;
        }

        Platform.runLater(() -> {
            Node content = contentScroll.getContent();
            if (content == null) {
                return;
            }
            double maxScroll = content.getBoundsInLocal().getHeight()
                    - contentScroll.getViewportBounds().getHeight();
            if (maxScroll <= 0) {
                return;
            }
            Bounds targetSceneBounds = target.localToScene(target.getBoundsInLocal());
            Bounds contentSceneBounds = content.localToScene(content.getBoundsInLocal());
            double currentTargetY = targetSceneBounds.getMinY() - contentSceneBounds.getMinY();
            double contentY = currentTargetY + contentScroll.getVvalue() * maxScroll;
            contentScroll.setVvalue(Math.max(0, Math.min(1, contentY / maxScroll)));
        });
    }

    private Node summaryCard(String label, Label valueLabel, String detail, String accentClass) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("summary-card", accentClass);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setPrefWidth(210);
        Label eyebrow = new Label(label);
        eyebrow.getStyleClass().add("card-eyebrow");
        valueLabel.getStyleClass().add("card-value");
        Label caption = new Label(detail);
        caption.getStyleClass().add("card-detail");
        card.getChildren().addAll(eyebrow, valueLabel, caption);
        return card;
    }

    private Node createTransferPanel() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(20));
        panel.setPrefWidth(370);
        Label title = new Label("Transfer ownership");
        title.getStyleClass().add("section-title");
        Label caption = new Label("The registry verifies the current owner before writing a new ledger event.");
        caption.getStyleClass().add("section-caption");
        caption.setWrapText(true);

        transferToken.setPromptText("Choose a token");
        transferToken.setMaxWidth(Double.MAX_VALUE);
        transferToken.setConverter(new StringConverter<>() {
            @Override
            public String toString(String tokenId) {
                if (tokenId == null) {
                    return "";
                }
                Token token = service.registry().getToken(tokenId);
                return token == null ? tokenId : tokenId + "  ·  " + token.asset().type().label();
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        });
        transferToken.getStyleClass().add("field-control");
        recipientField.setPromptText("New owner");
        recipientField.getStyleClass().add("field-control");
        Button transferButton = new Button("Validate and transfer");
        transferButton.getStyleClass().add("secondary-button");
        transferButton.setMaxWidth(Double.MAX_VALUE);
        transferButton.setOnAction(event -> handleTransfer());
        statusLabel.getStyleClass().add("status-label");

        panel.getChildren().addAll(
                title,
                caption,
                fieldLabel("TOKEN", transferToken),
                fieldLabel("RECIPIENT", recipientField),
                transferButton
        );
        return panel;
    }

    private Node createLedgerPanel() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(20));
        Label title = new Label("Recent ledger activity");
        title.getStyleClass().add("section-title");
        Label caption = new Label("Each event links to the previous SHA-256 hash.");
        caption.getStyleClass().add("section-caption");
        configureTransactionTable();
        VBox.setVgrow(transactionTable, Priority.ALWAYS);
        panel.getChildren().addAll(title, caption, transactionTable);
        return panel;
    }

    private Node fieldLabel(String label, Node field) {
        VBox wrapper = new VBox(6);
        Label fieldName = new Label(label);
        fieldName.getStyleClass().add("field-label");
        wrapper.getChildren().addAll(fieldName, field);
        return wrapper;
    }

    private void configureAssetTable() {
        TableColumn<AssetRow, String> tokenColumn = textColumn("TOKEN ID", AssetRow::tokenId);
        TableColumn<AssetRow, String> typeColumn = textColumn("TYPE", AssetRow::type);
        TableColumn<AssetRow, String> valueColumn = textColumn("FACE VALUE", AssetRow::value);
        TableColumn<AssetRow, String> ownerColumn = textColumn("CURRENT OWNER", AssetRow::owner);
        TableColumn<AssetRow, String> detailColumn = textColumn("DETAIL", AssetRow::detail);
        assetTable.getColumns().addAll(tokenColumn, typeColumn, valueColumn, ownerColumn, detailColumn);
        assetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        assetTable.setPlaceholder(new Label("No assets match the current filter."));
        assetTable.getStyleClass().add("data-table");
        valueColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                getStyleClass().add("money-cell");
            }
        });
    }

    private void configureTransactionTable() {
        TableColumn<TransactionRow, String> timeColumn = textColumn("TIME", TransactionRow::time);
        TableColumn<TransactionRow, String> tokenColumn = textColumn("TOKEN", TransactionRow::token);
        TableColumn<TransactionRow, String> transferColumn = textColumn("TRANSFER", TransactionRow::transfer);
        TableColumn<TransactionRow, String> hashColumn = textColumn("HASH", TransactionRow::hash);
        transactionTable.getColumns().addAll(timeColumn, tokenColumn, transferColumn, hashColumn);
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        transactionTable.setPlaceholder(new Label("No ledger activity yet."));
        transactionTable.getStyleClass().add("data-table");
    }

    private <T> TableColumn<T, String> textColumn(
            String title,
            java.util.function.Function<T, String> value
    ) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void refresh() {
        refreshAssetTable();
        refreshTransactionTable();
        refreshTransferTokens();

        assetCount.setText(Integer.toString(service.registry().size()));
        BigDecimal portfolioValue = service.tokens().stream()
                .map(token -> token.asset().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalValue.setText(CURRENCY.format(portfolioValue));
        transactionCount.setText(Integer.toString(service.ledger().size()));
        ownerCount.setText(Integer.toString(
                service.tokens().stream()
                        .map(token -> service.registry().currentOwner(token.tokenId()))
                        .collect(Collectors.toSet())
                        .size()
        ));
        statusLabel.setText(service.ledger().validateIntegrity()
                ? "Ledger integrity verified"
                : "Ledger integrity check failed");
    }

    private void refreshAssetTable() {
        String search = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedType = typeFilter.getValue();
        List<AssetRow> rows = service.tokens().stream()
                .map(this::toAssetRow)
                .filter(row -> selectedType == null
                        || selectedType.equals("All types")
                        || row.type().equals(selectedType))
                .filter(row -> search.isBlank()
                        || row.tokenId().toLowerCase(Locale.ROOT).contains(search)
                        || row.assetId().toLowerCase(Locale.ROOT).contains(search)
                        || row.owner().toLowerCase(Locale.ROOT).contains(search)
                        || row.detail().toLowerCase(Locale.ROOT).contains(search))
                .toList();
        assetTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void refreshTransactionTable() {
        List<TransactionRow> rows = service.transactions().stream()
                .sorted(Comparator.comparing(Transaction::timestamp).reversed())
                .limit(6)
                .map(transaction -> new TransactionRow(
                        DATE_TIME.format(transaction.timestamp()),
                        transaction.tokenId(),
                        transaction.fromOwner() + "  →  " + transaction.toOwner(),
                        transaction.hash().substring(0, 12) + "…"
                ))
                .toList();
        transactionTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void refreshTransferTokens() {
        String selected = transferToken.getValue();
        transferToken.setItems(FXCollections.observableArrayList(
                service.tokens().stream().map(Token::tokenId).toList()
        ));
        if (selected != null && transferToken.getItems().contains(selected)) {
            transferToken.setValue(selected);
        } else if (!transferToken.getItems().isEmpty()) {
            transferToken.getSelectionModel().selectFirst();
        }
    }

    private AssetRow toAssetRow(Token token) {
        String detail = token.asset().metadata().entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("  ·  "));
        return new AssetRow(
                token.tokenId(),
                token.asset().id(),
                token.asset().type().label(),
                CURRENCY.format(token.asset().value()),
                service.registry().currentOwner(token.tokenId()),
                detail
        );
    }

    private void handleTransfer() {
        String tokenId = transferToken.getValue();
        String recipient = recipientField.getText();
        if (tokenId == null || recipient == null || recipient.isBlank()) {
            showError("Choose a token and enter a recipient before transferring.");
            return;
        }
        try {
            service.transfer(tokenId, recipient);
            recipientField.clear();
            refresh();
            showInfo("Transfer recorded", "The ownership change was validated and appended to the ledger.");
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void showRegisterDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register asset");
        dialog.setHeaderText("Create a new tokenized asset");
        ButtonType save = new ButtonType("Register");
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setPadding(new Insets(16, 0, 8, 0));
        ComboBox<AssetType> type = new ComboBox<>(FXCollections.observableArrayList(AssetType.values()));
        type.getSelectionModel().selectFirst();
        TextField value = new TextField();
        value.setPromptText("0.00");
        TextField owner = new TextField();
        owner.setPromptText("Organization or person");
        TextField detail = new TextField();
        detail.setPromptText("e.g. serial: EQ-1001");
        form.addRow(0, new Label("Type"), type);
        form.addRow(1, new Label("Face value"), value);
        form.addRow(2, new Label("Owner"), owner);
        form.addRow(3, new Label("Metadata"), detail);
        dialog.getDialogPane().setContent(form);
        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                BigDecimal amount = new BigDecimal(value.getText().trim());
                if (owner.getText().isBlank()) {
                    throw new IllegalArgumentException("Owner is required");
                }
                Map<String, String> metadata = detail.getText().isBlank()
                        ? Map.of()
                        : Map.of("detail", detail.getText().trim());
                service.registerAsset(type.getValue(), amount, owner.getText(), metadata);
                refresh();
            } catch (Exception exception) {
                event.consume();
                showError(exception.getMessage());
            }
        });
        dialog.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("AssetLedger");
        alert.setHeaderText("Action could not be completed");
        alert.setContentText(message == null ? "An unexpected error occurred." : message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("AssetLedger");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record AssetRow(
            String tokenId,
            String assetId,
            String type,
            String value,
            String owner,
            String detail
    ) {
    }

    private record TransactionRow(String time, String token, String transfer, String hash) {
    }
}