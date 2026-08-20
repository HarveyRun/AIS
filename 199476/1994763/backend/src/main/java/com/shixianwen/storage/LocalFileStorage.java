package com.shixianwen.storage;

import com.shixianwen.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {
    private final Path root;
    private final FileTypeDetector fileTypeDetector;
    private final LocalMediaSigner localMediaSigner;

    public LocalFileStorage(
        @Value("${app.storage.root}") String root,
        FileTypeDetector fileTypeDetector,
        LocalMediaSigner localMediaSigner
    ) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.fileTypeDetector = fileTypeDetector;
        this.localMediaSigner = localMediaSigner;
    }

    @Override
    public StoredFile store(MultipartFile file, String folder, StorageVisibility visibility) {
        if (file.isEmpty()) throw BusinessException.badRequest("文件不能为空");
        FileTypeDetector.DetectedFile detected = fileTypeDetector.detect(file);
        if ("UNKNOWN".equals(detected.kind())) {
            throw BusinessException.badRequest("不支持该文件类型");
        }
        String extension = detected.extension();
        String key = visibility.folder() + "/" + folder + "/"
            + UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) throw BusinessException.badRequest("文件路径不正确");

        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String accessUrl = visibility == StorageVisibility.PUBLIC
                ? accessUrl(key, visibility)
                : null;
            return new StoredFile(key, accessUrl, detected.contentType(), file.getSize());
        } catch (IOException exception) {
            throw new IllegalStateException("文件保存失败", exception);
        }
    }

    @Override
    public String accessUrl(String storageKey, StorageVisibility visibility) {
        String normalized = storageKey.replace('\\', '/');
        return visibility == StorageVisibility.PUBLIC
            ? "/uploads/" + normalized
            : localMediaSigner.signedUrl(normalized);
    }

}
