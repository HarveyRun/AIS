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
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "oss")
public class OssFileStorage implements FileStorage {
    private final OSS client;
    private final String publicBucket;
    private final String privateBucket;
    private final String publicDomain;
    private final Duration privateUrlValidity;
    private final FileTypeDetector fileTypeDetector;

    public OssFileStorage(ThirdPartySettings settings, FileTypeDetector fileTypeDetector) {
        this.fileTypeDetector = fileTypeDetector;
        String endpoint = settings.value("app.storage.oss.endpoint", "oss.endpoint");
        String accessKeyId = settings.value("app.storage.oss.access-key-id", "oss.accessKeyId");
        String accessKeySecret = settings.value("app.storage.oss.access-key-secret", "oss.accessKeySecret");
        String configuredPublicBucket = settings.value("app.storage.oss.public-bucket");
        this.publicBucket = hasText(configuredPublicBucket)
            ? configuredPublicBucket
            : settings.value("app.storage.oss.bucket", "oss.bucket");
        this.privateBucket = settings.value("app.storage.oss.private-bucket");
        this.publicDomain = normalizePublicDomain(
            settings.value("app.storage.oss.public-domain", "oss.domain")
        );
        this.privateUrlValidity = Duration.ofMinutes(parsePositiveLong(
            settings.value("app.storage.oss.private-url-valid-minutes"),
            10L
        ));
        if (!hasText(endpoint) || !hasText(accessKeyId) || !hasText(accessKeySecret)
            || !hasText(publicBucket) || !hasText(privateBucket) || !hasText(publicDomain)) {
            throw new IllegalStateException("OSS配置不完整：必须配置公开Bucket、私有Bucket和公开访问域名");
        }
        this.client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    @Override
    public StoredFile store(MultipartFile file, String folder, StorageVisibility visibility) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("文件不能为空");
        }
        FileTypeDetector.DetectedFile detected = fileTypeDetector.detect(file);
        if ("UNKNOWN".equals(detected.kind())) {
            throw BusinessException.badRequest("不支持该文件类型");
        }
        String extension = detected.extension();
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String safeFolder = folder.replaceAll("[^a-zA-Z0-9/_-]", "");
        String key = visibility.folder() + '/' + safeFolder + '/' + date + '/'
            + UUID.randomUUID().toString().replace("-", "") + extension;
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(detected.contentType());
            client.putObject(bucketFor(visibility), key, file.getInputStream(), metadata);
            String accessUrl = visibility == StorageVisibility.PUBLIC
                ? accessUrl(key, visibility)
                : null;
            return new StoredFile(key, accessUrl, detected.contentType(), file.getSize());
        } catch (IOException | RuntimeException exception) {
            throw BusinessException.serviceUnavailable("文件上传失败，请稍后重试");
        }
    }

    @Override
    public String accessUrl(String storageKey, StorageVisibility visibility) {
        if (!hasText(storageKey)) return null;
        if (visibility == StorageVisibility.PUBLIC) {
            return publicDomain + '/' + storageKey;
        }
        Date expiresAt = Date.from(Instant.now().plus(privateUrlValidity));
        return client.generatePresignedUrl(privateBucket, storageKey, expiresAt).toString();
    }

    private String bucketFor(StorageVisibility visibility) {
        return visibility == StorageVisibility.PUBLIC ? publicBucket : privateBucket;
    }

    @PreDestroy
    public void close() {
        client.shutdown();
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

    private static long parsePositiveLong(String value, long fallback) {
        if (!hasText(value)) return fallback;
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
