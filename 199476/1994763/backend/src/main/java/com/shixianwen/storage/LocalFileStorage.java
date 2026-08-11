package com.shixianwen.storage;

import com.shixianwen.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorage implements FileStorage {
    private final Path root;

    public LocalFileStorage(@Value("${app.storage.root}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file, String folder) {
        if (file.isEmpty()) throw BusinessException.badRequest("文件不能为空");
        String extension = extensionOf(file.getOriginalFilename());
        String key = folder + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) throw BusinessException.badRequest("文件路径不正确");

        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(key, "/uploads/" + key.replace('\\', '/'), file.getContentType(), file.getSize());
        } catch (IOException exception) {
            throw new IllegalStateException("文件保存失败", exception);
        }
    }

    private static String extensionOf(String filename) {
        if (filename == null) return "";
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) return "";
        String extension = filename.substring(index).toLowerCase();
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }
}
