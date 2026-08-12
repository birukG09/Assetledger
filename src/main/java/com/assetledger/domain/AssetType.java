package com.assetledger.domain;

public enum AssetType {
    INVENTORY("Inventory"),
    INVOICE("Invoice"),
    EQUIPMENT("Equipment");

    private final String label;

    AssetType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}