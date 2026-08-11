package com.shixianwen.storage;

public record StoredFile(String storageKey, String publicUrl, String contentType, long size) {
}
