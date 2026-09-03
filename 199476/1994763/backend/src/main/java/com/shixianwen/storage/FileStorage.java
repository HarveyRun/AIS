package com.shixianwen.storage;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorage {
    StoredFile store(MultipartFile file, String folder, StorageVisibility visibility);

    default StoredFile store(Path file, String originalName, String folder, StorageVisibility visibility) {
        return store(new PathMultipartFile(file, originalName), folder, visibility);
    }

    void copyTo(String storageKey, StorageVisibility visibility, Path destination);

    String accessUrl(String storageKey, StorageVisibility visibility);
}
