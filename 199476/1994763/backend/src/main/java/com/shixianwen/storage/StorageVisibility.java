package com.shixianwen.storage;

public enum StorageVisibility {
    PUBLIC("public"),
    PRIVATE("private");

    private final String folder;

    StorageVisibility(String folder) {
        this.folder = folder;
    }

    public String folder() {
        return folder;
    }
}
