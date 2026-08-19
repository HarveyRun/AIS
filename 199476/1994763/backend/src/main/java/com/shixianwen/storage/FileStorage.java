package com.shixianwen.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    StoredFile store(MultipartFile file, String folder, StorageVisibility visibility);

    String accessUrl(String storageKey, StorageVisibility visibility);
}
