package com.shixianwen.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.shixianwen.common.BusinessException;
import com.shixianwen.integration.ThirdPartySettings;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "oss")
public class OssFileStorage implements FileStorage {
    private final OSS client;
    private final String bucket;
    private final String publicDomain;

    public OssFileStorage(ThirdPartySettings settings) {
        String endpoint = settings.value("app.storage.oss.endpoint", "oss.endpoint");
        String accessKeyId = settings.value("app.storage.oss.access-key-id", "oss.accessKeyId");
        String accessKeySecret = settings.value("app.storage.oss.access-key-secret", "oss.accessKeySecret");
        this.bucket = settings.value("app.storage.oss.bucket", "oss.bucket");
        this.publicDomain = normalizePublicDomain(
            settings.value("app.storage.oss.public-domain", "oss.domain")
        );
        if (!hasText(endpoint) || !hasText(accessKeyId) || !hasText(accessKeySecret)
            || !hasText(bucket) || !hasText(publicDomain)) {
            throw new IllegalStateException("OSS配置不完整");
        }
        this.client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    @Override
    public StoredFile store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("文件不能为空");
        }
        String extension = extensionOf(file.getOriginalFilename());
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String safeFolder = folder.replaceAll("[^a-zA-Z0-9/_-]", "");
        String key = safeFolder + '/' + date + '/'
            + UUID.randomUUID().toString().replace("-", "") + extension;
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            if (hasText(file.getContentType())) {
                metadata.setContentType(file.getContentType());
            }
            client.putObject(bucket, key, file.getInputStream(), metadata);
            return new StoredFile(key, publicDomain + '/' + key, file.getContentType(), file.getSize());
        } catch (IOException | RuntimeException exception) {
            throw BusinessException.serviceUnavailable("文件上传失败，请稍后重试");
        }
    }

    @PreDestroy
    public void close() {
        client.shutdown();
    }

    private static String extensionOf(String filename) {
        if (filename == null) return "";
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) return "";
        String extension = filename.substring(index).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) return "";
        return value.replaceAll("/+$", "");
    }

    private static String normalizePublicDomain(String value) {
        String normalized = trimTrailingSlash(value);
        if (normalized.startsWith("http://")) {
            return "https://" + normalized.substring("http://".length());
        }
        return normalized;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
