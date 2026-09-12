package com.shixianwen.banner;

import com.shixianwen.admin.AdminAuditLog;
import com.shixianwen.admin.AdminAuditLogRepository;
import com.shixianwen.admin.AdminUser;
import com.shixianwen.common.BusinessException;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HomeBannerService {
    private static final String TEXT_ONLY = "TEXT_ONLY";
    private static final String IMAGE_ONLY = "IMAGE_ONLY";
    private static final String IMAGE_TEXT = "IMAGE_TEXT";
    private static final Set<String> DISPLAY_MODES = Set.of(TEXT_ONLY, IMAGE_ONLY, IMAGE_TEXT);
    private static final Set<String> ACTION_TYPES = Set.of(
        "NONE",
        "MY_EXPERIENCES",
        "PLATFORM_INTRODUCTION",
        "FIRST_EXPERIENCE_REWARD"
    );
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

    private final HomeBannerRepository repository;
    private final AdminAuditLogRepository auditLogRepository;
    private final FileStorage fileStorage;

    @Transactional(readOnly = true)
    public PageResult list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<HomeBanner> result = repository.findAllByDeletedFalse(
            PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.desc("id"))
            )
        );
        return new PageResult(
            result.getContent().stream().map(BannerView::from).toList(),
            result.getTotalElements(),
            safePage,
            safeSize
        );
    }

    @Transactional(readOnly = true)
    public List<PublicBannerView> publicBanners() {
        LocalDateTime now = LocalDateTime.now();
        return repository
            .findAllByDeletedFalseAndEnabledTrueAndStartAtLessThanEqualAndEndAtGreaterThanOrderBySortOrderAscIdAsc(
                now,
                now
            )
            .stream()
            .map(PublicBannerView::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public BannerAvailabilityView availability(Long id) {
        HomeBanner banner = repository.findById(id).orElse(null);
        if (banner == null || banner.isDeleted() || !banner.isEnabled()) {
            return BannerAvailabilityView.unavailable("OFFLINE", "该内容已下架");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(banner.getStartAt())) {
            return BannerAvailabilityView.unavailable("NOT_STARTED", "该内容暂未开始");
        }
        if (!now.isBefore(banner.getEndAt())) {
            return BannerAvailabilityView.unavailable("ENDED", "该内容已结束");
        }
        return new BannerAvailabilityView(
            true,
            "AVAILABLE",
            null,
            PublicBannerView.from(banner)
        );
    }

    @Transactional(readOnly = true)
    public boolean isActionAvailable(String actionType) {
        LocalDateTime now = LocalDateTime.now();
        return repository
            .existsByActionTypeAndDeletedFalseAndEnabledTrueAndStartAtLessThanEqualAndEndAtGreaterThan(
                actionType,
                now,
                now
            );
    }

    public ImageUploadView uploadImage(MultipartFile image) {
        validateImage(image);
        StoredFile stored = fileStorage.store(image, "banners", StorageVisibility.PUBLIC);
        return new ImageUploadView(stored.publicUrl());
    }

    @Transactional
    public BannerView create(AdminUser admin, SaveCommand command, String ipAddress) {
        ValidatedCommand value = validate(command);
        HomeBanner banner = new HomeBanner();
        apply(banner, value, admin);
        banner = repository.save(banner);
        audit(admin, "CREATE_HOME_BANNER", banner, ipAddress);
        return BannerView.from(banner);
    }

    @Transactional
    public BannerView update(
        AdminUser admin,
        Long id,
        SaveCommand command,
        String ipAddress
    ) {
        HomeBanner banner = active(id);
        apply(banner, validate(command), admin);
        banner = repository.save(banner);
        audit(admin, "UPDATE_HOME_BANNER", banner, ipAddress);
        return BannerView.from(banner);
    }

    @Transactional
    public BannerView setEnabled(
        AdminUser admin,
        Long id,
        boolean enabled,
        String ipAddress
    ) {
        HomeBanner banner = active(id);
        banner.setEnabled(enabled);
        banner.setUpdatedByAdmin(admin);
        banner = repository.save(banner);
        audit(
            admin,
            enabled ? "ENABLE_HOME_BANNER" : "DISABLE_HOME_BANNER",
            banner,
            ipAddress
        );
        return BannerView.from(banner);
    }

    @Transactional
    public void delete(AdminUser admin, Long id, String ipAddress) {
        HomeBanner banner = active(id);
        banner.setEnabled(false);
        banner.setDeleted(true);
        banner.setDeletedAt(LocalDateTime.now());
        banner.setUpdatedByAdmin(admin);
        repository.save(banner);
        audit(admin, "DELETE_HOME_BANNER", banner, ipAddress);
    }

    private HomeBanner active(Long id) {
        return repository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> BusinessException.notFound("Banner不存在"));
    }

    private void apply(HomeBanner banner, ValidatedCommand value, AdminUser admin) {
        banner.setDisplayMode(value.displayMode());
        banner.setLabelText(value.labelText());
        banner.setTitle(value.title());
        banner.setDescription(value.description());
        banner.setImageUrl(value.imageUrl());
        banner.setActionType(value.actionType());
        banner.setSortOrder(value.sortOrder());
        banner.setStartAt(value.startAt());
        banner.setEndAt(value.endAt());
        banner.setEnabled(value.enabled());
        banner.setUpdatedByAdmin(admin);
    }

    private ValidatedCommand validate(SaveCommand command) {
        String mode = text(command.displayMode()).toUpperCase(Locale.ROOT);
        if (!DISPLAY_MODES.contains(mode)) {
            throw BusinessException.badRequest("Banner展示方式不正确");
        }
        if (command.sortOrder() < 0 || command.sortOrder() > 9999) {
            throw BusinessException.badRequest("排序必须在0到9999之间");
        }
        if (command.startAt() == null || command.endAt() == null) {
            throw BusinessException.badRequest("请设置开始时间和结束时间");
        }
        if (!command.endAt().isAfter(command.startAt())) {
            throw BusinessException.badRequest("结束时间必须晚于开始时间");
        }

        String label = optional(command.labelText(), 30, "标签最多30个字");
        String title = optional(command.title(), 80, "标题最多80个字");
        String description = optional(command.description(), 200, "说明最多200个字");
        String imageUrl = optional(command.imageUrl(), 500, "图片地址过长");
        String actionType = text(command.actionType()).toUpperCase(Locale.ROOT);
        if (actionType.isEmpty()) actionType = "NONE";
        if (!ACTION_TYPES.contains(actionType)) {
            throw BusinessException.badRequest("Banner点击去向不正确");
        }

        if (TEXT_ONLY.equals(mode)) {
            if (title == null) throw BusinessException.badRequest("请输入Banner标题");
            imageUrl = null;
        } else if (IMAGE_ONLY.equals(mode)) {
            if (imageUrl == null) throw BusinessException.badRequest("请上传Banner图片");
            label = null;
            title = null;
            description = null;
        } else {
            if (imageUrl == null) throw BusinessException.badRequest("请上传Banner图片");
            if (title == null) throw BusinessException.badRequest("请输入Banner标题");
        }

        return new ValidatedCommand(
            mode,
            label,
            title,
            description,
            imageUrl,
            actionType,
            command.sortOrder(),
            command.startAt(),
            command.endAt(),
            command.enabled()
        );
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw BusinessException.badRequest("请选择Banner图片");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw BusinessException.badRequest("Banner图片不能超过10MB");
        }
        String contentType = text(image.getContentType()).toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(contentType)) {
            throw BusinessException.badRequest("仅支持JPG、PNG或WebP图片");
        }
    }

    private String optional(String source, int maxLength, String message) {
        String value = text(source);
        if (value.isEmpty()) return null;
        if (value.length() > maxLength) throw BusinessException.badRequest(message);
        return value;
    }

    private String text(String source) {
        return source == null ? "" : source.trim();
    }

    private void audit(
        AdminUser admin,
        String action,
        HomeBanner banner,
        String ipAddress
    ) {
        AdminAuditLog audit = new AdminAuditLog();
        audit.setAdminUser(admin);
        audit.setAction(action);
        audit.setTargetType("HOME_BANNER");
        audit.setTargetId(String.valueOf(banner.getId()));
        audit.setDetail(banner.getDisplayMode() + " " + (banner.getTitle() == null ? "" : banner.getTitle()));
        audit.setIpAddress(ipAddress);
        auditLogRepository.save(audit);
    }

    public record SaveCommand(
        String displayMode,
        String labelText,
        String title,
        String description,
        String imageUrl,
        String actionType,
        int sortOrder,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean enabled
    ) {
    }

    private record ValidatedCommand(
        String displayMode,
        String labelText,
        String title,
        String description,
        String imageUrl,
        String actionType,
        int sortOrder,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean enabled
    ) {
    }

    public record ImageUploadView(String imageUrl) {
    }

    public record BannerView(
        Long id,
        String displayMode,
        String labelText,
        String title,
        String description,
        String imageUrl,
        String actionType,
        int sortOrder,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean enabled,
        String updatedBy,
        LocalDateTime updatedAt
    ) {
        static BannerView from(HomeBanner banner) {
            AdminUser admin = banner.getUpdatedByAdmin();
            return new BannerView(
                banner.getId(),
                banner.getDisplayMode(),
                banner.getLabelText(),
                banner.getTitle(),
                banner.getDescription(),
                banner.getImageUrl(),
                banner.getActionType(),
                banner.getSortOrder(),
                banner.getStartAt(),
                banner.getEndAt(),
                banner.isEnabled(),
                admin == null ? null : admin.getDisplayName(),
                banner.getUpdatedAt()
            );
        }
    }

    public record PublicBannerView(
        Long id,
        String displayMode,
        String labelText,
        String title,
        String description,
        String imageUrl,
        String actionType
    ) {
        static PublicBannerView from(HomeBanner banner) {
            return new PublicBannerView(
                banner.getId(),
                banner.getDisplayMode(),
                banner.getLabelText(),
                banner.getTitle(),
                banner.getDescription(),
                banner.getImageUrl(),
                banner.getActionType()
            );
        }
    }

    public record BannerAvailabilityView(
        boolean available,
        String state,
        String message,
        PublicBannerView banner
    ) {
        static BannerAvailabilityView unavailable(String state, String message) {
            return new BannerAvailabilityView(false, state, message, null);
        }
    }

    public record PageResult(List<BannerView> items, long total, int page, int size) {
    }
}
