package com.shixianwen.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalFileStorageTest {
    @TempDir
    Path root;

    @Test
    void separatesPublicAndPrivateFiles() throws Exception {
        FileTypeDetector detector = mock(FileTypeDetector.class);
        when(detector.detect(org.mockito.ArgumentMatchers.any(org.springframework.web.multipart.MultipartFile.class)))
            .thenReturn(new FileTypeDetector.DetectedFile("IMAGE", "image/png", ".png"));
        LocalFileStorage storage = new LocalFileStorage(
            root.toString(),
            detector,
            new LocalMediaSigner("test-signing-secret-123456")
        );

        StoredFile publicFile = storage.store(
            image("avatar.png"),
            "avatars/1",
            StorageVisibility.PUBLIC
        );
        StoredFile privateFile = storage.store(
            image("identity.png"),
            "certifications/100001",
            StorageVisibility.PRIVATE
        );

        assertThat(publicFile.storageKey()).startsWith("public/avatars/1/");
        assertThat(publicFile.publicUrl()).startsWith("/uploads/public/avatars/1/");
        assertThat(privateFile.storageKey()).startsWith("private/certifications/100001/");
        assertThat(privateFile.publicUrl()).isNull();
        assertThat(Files.exists(root.resolve(publicFile.storageKey()))).isTrue();
        assertThat(Files.exists(root.resolve(privateFile.storageKey()))).isTrue();
    }

    private MockMultipartFile image(String name) {
        return new MockMultipartFile("file", name, "image/png", new byte[] {1, 2, 3});
    }
}
