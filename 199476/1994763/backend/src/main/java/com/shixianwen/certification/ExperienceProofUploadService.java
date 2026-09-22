package com.shixianwen.certification;

import com.shixianwen.common.BusinessException;
import com.shixianwen.config.AppGlobalSettingService;
import com.shixianwen.storage.OssFileStorage;
import com.shixianwen.storage.StoredFile;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class ExperienceProofUploadService {
    private static final int PART_SIZE = 8 * 1024 * 1024;
    private static final long MAX_SIZE = 2L * 1024 * 1024 * 1024;
    private final ExperienceProofUploadRepository uploads;
    private final UserRepository users;
    private final ObjectProvider<OssFileStorage> ossProvider;
    private final ObjectProvider<AppGlobalSettingService> settingsProvider;

    public boolean directAvailable() {
        return ossProvider.getIfAvailable() != null;
    }

    public ExperienceProofUploadService(
        ExperienceProofUploadRepository uploads,
        UserRepository users,
        ObjectProvider<OssFileStorage> ossProvider,
        ObjectProvider<AppGlobalSettingService> settingsProvider
    ) {
        this.uploads = uploads;
        this.users = users;
        this.ossProvider = ossProvider;
        this.settingsProvider = settingsProvider;
    }

    @Transactional
    public UploadView initiate(User user, String originalName, long size) {
        OssFileStorage oss = oss();
        User owner = users.findWithLockById(user.getId())
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        long maxSize = maxSize();
        if (size < 1 || size > maxSize) {
            throw BusinessException.badRequest("证明资料不能超过" + (maxSize / 1024 / 1024) + "MB");
        }
        String name = safeName(originalName);
        String extension = name.toLowerCase(Locale.ROOT).endsWith(".zip") ? ".zip"
            : name.toLowerCase(Locale.ROOT).endsWith(".rar") ? ".rar" : null;
        if (extension == null) throw BusinessException.badRequest("证明资料仅支持 ZIP 或 RAR 压缩包");
        LocalDateTime now = LocalDateTime.now();
        long active = uploads.countByUserIdAndStatusAndExpiresAtAfter(owner.getId(), "UPLOADING", now)
            + uploads.countByUserIdAndStatusAndExpiresAtAfter(owner.getId(), "COMPLETE", now);
        if (active >= 3) throw BusinessException.badRequest("有未提交的证明资料，请先完成提交后再上传");

        String id = UUID.randomUUID().toString();
        String key = "private/" + ("TEST".equals(owner.getAccountType()) ? "test/" : "")
            + "certifications/" + owner.getUid() + "/direct/" + id + extension;
        String contentType = ".zip".equals(extension)
            ? "application/zip" : "application/vnd.rar";
        String ossUploadId = oss.initiatePrivateMultipart(key, contentType);
        ExperienceProofUpload upload = new ExperienceProofUpload();
        upload.setId(id);
        upload.setUserId(owner.getId());
        upload.setStorageKey(key);
        upload.setOssUploadId(ossUploadId);
        upload.setOriginalName(name);
        upload.setExpectedSize(size);
        upload.setPartSize(PART_SIZE);
        upload.setContentType(contentType);
        upload.setStatus("UPLOADING");
        upload.setCreatedAt(now);
        upload.setExpiresAt(now.plusHours(24));
        try {
            uploads.save(upload);
        } catch (RuntimeException exception) {
            try { oss.abortPrivateMultipart(key, ossUploadId); }
            catch (RuntimeException cleanupError) { log.warn("OSS multipart cleanup failed", cleanupError); }
            throw exception;
        }
        return view(upload);
    }

    @Transactional(readOnly = true)
    public PartUrl signPart(User user, String id, int partNumber) {
        ExperienceProofUpload upload = owned(user, id);
        requireUploading(upload);
        int partCount = partCount(upload);
        if (partNumber < 1 || partNumber > partCount) {
            throw BusinessException.badRequest("上传分片编号不正确");
        }
        String url = oss().signPrivatePart(upload.getStorageKey(), upload.getOssUploadId(), partNumber);
        if (!url.startsWith("https://")) {
            throw BusinessException.serviceUnavailable("OSS 上传地址必须使用 HTTPS");
        }
        return new PartUrl(partNumber, url);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public UploadView complete(User user, String id) {
        ExperienceProofUpload upload = owned(user, id);
        if ("COMPLETE".equals(upload.getStatus())) return view(upload);
        requireUploading(upload);
        OssFileStorage oss = oss();
        if (!oss.privateObjectExists(upload.getStorageKey())) {
            List<com.aliyun.oss.model.PartSummary> parts = oss.privateParts(
                upload.getStorageKey(), upload.getOssUploadId()
            );
            int expectedCount = partCount(upload);
            if (parts.size() != expectedCount) throw BusinessException.badRequest("证明资料尚未上传完成");
            long total = 0;
            for (int index = 0; index < parts.size(); index++) {
                var part = parts.get(index);
                long expected = Math.min(upload.getPartSize(), upload.getExpectedSize() - total);
                if (part.getPartNumber() != index + 1 || part.getSize() != expected) {
                    throw BusinessException.badRequest("证明资料分片不完整，请重新上传");
                }
                total += part.getSize();
            }
            if (total != upload.getExpectedSize()) throw BusinessException.badRequest("证明资料大小不一致");
            oss.completePrivateMultipart(upload.getStorageKey(), upload.getOssUploadId(), parts);
        }
        if (oss.privateObjectSize(upload.getStorageKey()) != upload.getExpectedSize()
            || !validHeader(oss.privateObjectHeader(upload.getStorageKey()), upload.getContentType())) {
            oss.deletePrivateObject(upload.getStorageKey());
            upload.setStatus("INVALID");
            uploads.save(upload);
            throw BusinessException.badRequest("文件内容不是有效的 ZIP 或 RAR 压缩包");
        }
        upload.setStatus("COMPLETE");
        upload.setCompletedAt(LocalDateTime.now());
        return view(uploads.save(upload));
    }

    @Transactional
    public void abort(User user, String id) {
        ExperienceProofUpload upload = owned(user, id);
        if ("CONSUMED".equals(upload.getStatus())) {
            throw BusinessException.badRequest("已提交的证明资料不能撤销上传");
        }
        if ("ABORTED".equals(upload.getStatus()) || "EXPIRED".equals(upload.getStatus())) return;
        OssFileStorage oss = oss();
        if ("UPLOADING".equals(upload.getStatus())) {
            try {
                oss.abortPrivateMultipart(upload.getStorageKey(), upload.getOssUploadId());
            } catch (RuntimeException exception) {
                log.debug("Multipart upload might already have completed, id={}", id, exception);
            }
        }
        oss.deletePrivateObject(upload.getStorageKey());
        upload.setStatus("ABORTED");
        uploads.save(upload);
    }

    /** Invoked inside the experience-submission transaction; a token can be attached only once. */
    @Transactional
    public BoundProof consume(User user, String id) {
        ExperienceProofUpload upload = owned(user, id);
        if (!"COMPLETE".equals(upload.getStatus()) || expired(upload)) {
            throw BusinessException.badRequest("证明资料上传已失效，请重新上传");
        }
        upload.setStatus("CONSUMED");
        upload.setConsumedAt(LocalDateTime.now());
        uploads.save(upload);
        return new BoundProof(
            new StoredFile(upload.getStorageKey(), null, upload.getContentType(), upload.getExpectedSize()),
            upload.getOriginalName()
        );
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.storage.oss.proof-upload-cleanup-ms:3600000}")
    public void cleanupExpired() {
        OssFileStorage oss = ossProvider.getIfAvailable();
        if (oss == null) return;
        for (ExperienceProofUpload upload : uploads.findTop100ByStatusInAndExpiresAtBeforeOrderByExpiresAtAsc(
            List.of("UPLOADING", "COMPLETE"), LocalDateTime.now()
        )) {
            try {
                if (oss.privateObjectExists(upload.getStorageKey())) {
                    oss.deletePrivateObject(upload.getStorageKey());
                } else if ("UPLOADING".equals(upload.getStatus())) {
                    oss.abortPrivateMultipart(upload.getStorageKey(), upload.getOssUploadId());
                }
                upload.setStatus("EXPIRED");
                uploads.save(upload);
            } catch (RuntimeException exception) {
                log.warn("Expired proof upload cleanup failed, id={}", upload.getId(), exception);
            }
        }
    }

    private ExperienceProofUpload owned(User user, String id) {
        return uploads.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> BusinessException.notFound("证明资料上传记录不存在"));
    }

    private void requireUploading(ExperienceProofUpload upload) {
        if (!"UPLOADING".equals(upload.getStatus()) || expired(upload)) {
            throw BusinessException.badRequest("证明资料上传已失效，请重新上传");
        }
    }

    private boolean expired(ExperienceProofUpload upload) {
        return !upload.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private int partCount(ExperienceProofUpload upload) {
        return (int) ((upload.getExpectedSize() + upload.getPartSize() - 1) / upload.getPartSize());
    }

    private UploadView view(ExperienceProofUpload upload) {
        return new UploadView(upload.getId(), upload.getPartSize(), partCount(upload), upload.getStatus());
    }

    private long maxSize() {
        AppGlobalSettingService settings = settingsProvider.getIfAvailable();
        return settings == null ? MAX_SIZE : Math.min(MAX_SIZE, settings.current().proofArchiveMaxBytes());
    }

    private OssFileStorage oss() {
        OssFileStorage storage = ossProvider.getIfAvailable();
        if (storage == null) throw BusinessException.serviceUnavailable("证明资料直传服务暂不可用");
        return storage;
    }

    private String safeName(String input) {
        if (input == null) return "";
        String result = input.replace('\\', '/');
        result = result.substring(result.lastIndexOf('/') + 1).trim();
        if (result.isBlank() || result.length() > 255
            || result.chars().anyMatch(character -> Character.isISOControl((char) character))) {
            throw BusinessException.badRequest("证明资料文件名不正确");
        }
        return result;
    }

    private boolean validHeader(byte[] header, String contentType) {
        if ("application/zip".equals(contentType)) {
            return header.length >= 4 && header[0] == 0x50 && header[1] == 0x4b
                && ((header[2] == 0x03 && header[3] == 0x04)
                    || (header[2] == 0x05 && header[3] == 0x06)
                    || (header[2] == 0x07 && header[3] == 0x08));
        }
        return header.length >= 7 && header[0] == 0x52 && header[1] == 0x61
            && header[2] == 0x72 && header[3] == 0x21 && header[4] == 0x1a
            && header[5] == 0x07 && (header[6] == 0x00 || header[6] == 0x01);
    }

    public record UploadView(String uploadId, int partSize, int partCount, String status) {}
    public record PartUrl(int partNumber, String url) {}
    public record BoundProof(StoredFile file, String originalName) {}
}
