package com.shixianwen.version;

import com.shixianwen.admin.AdminAuditLog;
import com.shixianwen.admin.AdminAuditLogRepository;
import com.shixianwen.admin.AdminUser;
import com.shixianwen.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AppVersionService {
    private static final String ANDROID = "ANDROID";

    private final AppVersionRepository repository;
    private final AdminAuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PageResult list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AppVersion> result = repository.findAllByDeletedFalse(
            PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("published"), Sort.Order.desc("versionCode"))
            )
        );
        return new PageResult(
            result.getContent().stream().map(VersionView::from).toList(),
            result.getTotalElements(),
            safePage,
            safeSize
        );
    }

    @Transactional(readOnly = true)
    public UpdateCheck check(String platform, int currentVersionCode) {
        String normalizedPlatform = normalizePlatform(platform);
        if (currentVersionCode < 1) {
            throw BusinessException.badRequest("当前版本号无效");
        }
        return repository
            .findFirstByPlatformAndPublishedTrueAndDeletedFalseOrderByVersionCodeDesc(
                normalizedPlatform
            )
            .filter(version -> currentVersionCode < version.getVersionCode())
            .map(version -> new UpdateCheck(
                true,
                currentVersionCode < version.getMinimumSupportedVersionCode(),
                version.getVersionName(),
                version.getVersionCode(),
                version.getMinimumSupportedVersionCode(),
                version.getTitle(),
                version.getUpdateContent(),
                version.getDownloadUrl()
            ))
            .orElse(UpdateCheck.none());
    }

    @Transactional
    public VersionView create(AdminUser admin, SaveCommand command, String ipAddress) {
        ValidatedCommand value = validate(command);
        if (repository.existsByPlatformAndVersionCodeAndDeletedFalse(
            value.platform(),
            value.versionCode()
        )) {
            throw BusinessException.badRequest("该版本号已经存在");
        }
        AppVersion version = new AppVersion();
        apply(version, value, admin);
        version = repository.save(version);
        audit(admin, "CREATE_APP_VERSION", version, ipAddress);
        return VersionView.from(version);
    }

    @Transactional
    public VersionView update(
        AdminUser admin,
        Long id,
        SaveCommand command,
        String ipAddress
    ) {
        AppVersion version = active(id);
        ValidatedCommand value = validate(command);
        if (repository.existsByPlatformAndVersionCodeAndDeletedFalseAndIdNot(
            value.platform(),
            value.versionCode(),
            id
        )) {
            throw BusinessException.badRequest("该版本号已经存在");
        }
        apply(version, value, admin);
        version = repository.save(version);
        audit(admin, "UPDATE_APP_VERSION", version, ipAddress);
        return VersionView.from(version);
    }

    @Transactional
    public VersionView publish(AdminUser admin, Long id, String ipAddress) {
        AppVersion version = active(id);
        repository.unpublishOthers(version.getPlatform(), version.getId());
        version.setPublished(true);
        version.setPublishedAt(LocalDateTime.now());
        version.setUpdatedByAdmin(admin);
        version = repository.save(version);
        audit(admin, "PUBLISH_APP_VERSION", version, ipAddress);
        return VersionView.from(version);
    }

    @Transactional
    public VersionView unpublish(AdminUser admin, Long id, String ipAddress) {
        AppVersion version = active(id);
        version.setPublished(false);
        version.setPublishedAt(null);
        version.setUpdatedByAdmin(admin);
        version = repository.save(version);
        audit(admin, "UNPUBLISH_APP_VERSION", version, ipAddress);
        return VersionView.from(version);
    }

    @Transactional
    public void delete(AdminUser admin, Long id, String ipAddress) {
        AppVersion version = active(id);
        version.setPublished(false);
        version.setPublishedAt(null);
        version.setDeleted(true);
        version.setDeletedAt(LocalDateTime.now());
        version.setUpdatedByAdmin(admin);
        repository.save(version);
        audit(admin, "DELETE_APP_VERSION", version, ipAddress);
    }

    private AppVersion active(Long id) {
        return repository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> BusinessException.notFound("版本记录不存在"));
    }

    private void apply(AppVersion version, ValidatedCommand value, AdminUser admin) {
        version.setPlatform(value.platform());
        version.setVersionName(value.versionName());
        version.setVersionCode(value.versionCode());
        version.setMinimumSupportedVersionCode(value.minimumSupportedVersionCode());
        version.setTitle(value.title());
        version.setUpdateContent(value.updateContent());
        version.setDownloadUrl(value.downloadUrl());
        version.setUpdatedByAdmin(admin);
    }

    private ValidatedCommand validate(SaveCommand command) {
        String platform = normalizePlatform(command.platform());
        int versionCode = command.versionCode();
        int minimum = command.minimumSupportedVersionCode();
        if (versionCode < 1) throw BusinessException.badRequest("版本号必须大于0");
        if (minimum < 1 || minimum > versionCode) {
            throw BusinessException.badRequest("最低可用版本号必须在1到当前版本号之间");
        }
        return new ValidatedCommand(
            platform,
            required(command.versionName(), 30, "版本名称不能为空"),
            versionCode,
            minimum,
            required(command.title(), 80, "更新标题不能为空"),
            required(command.updateContent(), 1000, "更新内容不能为空"),
            downloadUrl(command.downloadUrl())
        );
    }

    private String normalizePlatform(String platform) {
        String value = platform == null ? "" : platform.trim().toUpperCase(Locale.ROOT);
        if (!ANDROID.equals(value)) {
            throw BusinessException.badRequest("暂时只支持Android版本");
        }
        return value;
    }

    private String required(String source, int maxLength, String message) {
        String value = source == null ? "" : source.trim();
        if (value.isEmpty()) throw BusinessException.badRequest(message);
        if (value.length() > maxLength) throw BusinessException.badRequest(message);
        return value;
    }

    private String downloadUrl(String source) {
        String value = required(source, 500, "下载地址不能为空");
        try {
            URI uri = URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw BusinessException.badRequest("请输入正确的下载地址");
        }
        return value;
    }

    private void audit(
        AdminUser admin,
        String action,
        AppVersion version,
        String ipAddress
    ) {
        AdminAuditLog audit = new AdminAuditLog();
        audit.setAdminUser(admin);
        audit.setAction(action);
        audit.setTargetType("APP_VERSION");
        audit.setTargetId(String.valueOf(version.getId()));
        audit.setDetail(
            version.getPlatform() + " " + version.getVersionName()
                + "(" + version.getVersionCode() + ")"
        );
        audit.setIpAddress(ipAddress);
        auditLogRepository.save(audit);
    }

    public record SaveCommand(
        String platform,
        String versionName,
        int versionCode,
        int minimumSupportedVersionCode,
        String title,
        String updateContent,
        String downloadUrl
    ) {
    }

    private record ValidatedCommand(
        String platform,
        String versionName,
        int versionCode,
        int minimumSupportedVersionCode,
        String title,
        String updateContent,
        String downloadUrl
    ) {
    }

    public record VersionView(
        Long id,
        String platform,
        String versionName,
        int versionCode,
        int minimumSupportedVersionCode,
        String title,
        String updateContent,
        String downloadUrl,
        boolean published,
        LocalDateTime publishedAt,
        String updatedBy,
        LocalDateTime updatedAt
    ) {
        static VersionView from(AppVersion version) {
            AdminUser admin = version.getUpdatedByAdmin();
            return new VersionView(
                version.getId(),
                version.getPlatform(),
                version.getVersionName(),
                version.getVersionCode(),
                version.getMinimumSupportedVersionCode(),
                version.getTitle(),
                version.getUpdateContent(),
                version.getDownloadUrl(),
                version.isPublished(),
                version.getPublishedAt(),
                admin == null ? null : admin.getDisplayName(),
                version.getUpdatedAt()
            );
        }
    }

    public record PageResult(List<VersionView> items, long total, int page, int size) {
    }

    public record UpdateCheck(
        boolean hasUpdate,
        boolean forceUpdate,
        String latestVersionName,
        Integer latestVersionCode,
        Integer minimumSupportedVersionCode,
        String title,
        String updateContent,
        String downloadUrl
    ) {
        static UpdateCheck none() {
            return new UpdateCheck(false, false, null, null, null, null, null, null);
        }
    }
}
