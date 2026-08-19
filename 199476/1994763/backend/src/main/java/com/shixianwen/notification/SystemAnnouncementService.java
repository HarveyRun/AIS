package com.shixianwen.notification;

import com.shixianwen.admin.AdminAuditLog;
import com.shixianwen.admin.AdminAuditLogRepository;
import com.shixianwen.admin.AdminUser;
import com.shixianwen.common.BusinessException;
import com.shixianwen.realtime.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemAnnouncementService {
    private static final List<String> SAVE_MODES = List.of(
        "DRAFT",
        "NOW",
        "SCHEDULED"
    );

    private final SystemAnnouncementRepository repository;
    private final JdbcTemplate jdbc;
    private final AdminAuditLogRepository auditLogs;
    private final RealtimePublisher realtime;
    private final SystemAnnouncementScheduler scheduler;

    @Transactional(readOnly = true)
    public PageResult list(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Page<SystemAnnouncement> result = repository.findAllByDeletedAtIsNull(
            PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"))
            )
        );
        return new PageResult(
            result.getContent().stream().map(AnnouncementView::from).toList(),
            result.getTotalElements(),
            safePage,
            safeSize
        );
    }

    @Transactional
    public AnnouncementView create(
        AdminUser admin,
        SaveCommand command,
        String ipAddress
    ) {
        ValidatedCommand value = validate(command);
        SystemAnnouncement item = new SystemAnnouncement();
        item.setCreatedByAdmin(admin);
        item.setUpdatedByAdmin(admin);
        applyContent(item, value);
        applyMode(item, value, false);
        item = repository.saveAndFlush(item);
        if ("NOW".equals(value.mode())) {
            publishEntity(item, admin);
            item = repository.save(item);
            notifyPublished(item);
        } else if ("SCHEDULED".equals(value.mode())) {
            scheduler.schedule(item.getId(), item.getScheduledAt());
        }
        audit(admin, "CREATE_ANNOUNCEMENT", item, value.mode(), ipAddress);
        return AnnouncementView.from(item);
    }

    @Transactional
    public AnnouncementView update(
        AdminUser admin,
        Long id,
        SaveCommand command,
        String ipAddress
    ) {
        SystemAnnouncement item = active(id);
        if ("PUBLISHED".equals(item.getStatus())) {
            throw BusinessException.badRequest("已发布的通知不能编辑");
        }
        ValidatedCommand value = validate(command);
        scheduler.cancel(id);
        applyContent(item, value);
        item.setUpdatedByAdmin(admin);
        applyMode(item, value, true);
        if ("NOW".equals(value.mode())) {
            publishEntity(item, admin);
            notifyPublished(item);
        }
        item = repository.save(item);
        if ("SCHEDULED".equals(value.mode())) {
            scheduler.schedule(item.getId(), item.getScheduledAt());
        }
        audit(admin, "UPDATE_ANNOUNCEMENT", item, value.mode(), ipAddress);
        return AnnouncementView.from(item);
    }

    @Transactional
    public AnnouncementView publish(
        AdminUser admin,
        Long id,
        String ipAddress
    ) {
        SystemAnnouncement item = active(id);
        if ("PUBLISHED".equals(item.getStatus())) {
            throw BusinessException.badRequest("该通知已经发布");
        }
        scheduler.cancel(id);
        publishEntity(item, admin);
        item = repository.save(item);
        notifyPublished(item);
        audit(admin, "PUBLISH_ANNOUNCEMENT", item, "NOW", ipAddress);
        return AnnouncementView.from(item);
    }

    @Transactional
    public AnnouncementView withdraw(
        AdminUser admin,
        Long id,
        String ipAddress
    ) {
        SystemAnnouncement item = active(id);
        if (!List.of("PUBLISHED", "SCHEDULED").contains(item.getStatus())) {
            throw BusinessException.badRequest("当前状态不能撤回");
        }
        boolean wasPublished = "PUBLISHED".equals(item.getStatus());
        scheduler.cancel(id);
        item.setStatus("WITHDRAWN");
        item.setScheduledAt(null);
        item.setUpdatedByAdmin(admin);
        item = repository.save(item);
        if (wasPublished) notifyWithdrawn(item.getId());
        audit(admin, "WITHDRAW_ANNOUNCEMENT", item, "WITHDRAWN", ipAddress);
        return AnnouncementView.from(item);
    }

    @Transactional
    public void delete(
        AdminUser admin,
        Long id,
        String ipAddress
    ) {
        SystemAnnouncement item = active(id);
        boolean wasPublished = "PUBLISHED".equals(item.getStatus());
        scheduler.cancel(id);
        item.setDeletedAt(LocalDateTime.now());
        item.setUpdatedByAdmin(admin);
        repository.save(item);
        if (wasPublished) notifyWithdrawn(item.getId());
        audit(admin, "DELETE_ANNOUNCEMENT", item, "SOFT_DELETE", ipAddress);
    }

    @Transactional
    public void publishScheduled(Long id, LocalDateTime expectedTime) {
        SystemAnnouncement item = repository
            .findByIdAndDeletedAtIsNull(id)
            .orElse(null);
        if (item == null || !"SCHEDULED".equals(item.getStatus())) return;
        if (item.getScheduledAt() == null ||
            !item.getScheduledAt().isEqual(expectedTime)) return;
        if (item.getScheduledAt().isAfter(LocalDateTime.now())) {
            scheduler.schedule(id, item.getScheduledAt());
            return;
        }
        publishEntity(item, item.getUpdatedByAdmin());
        repository.save(item);
        notifyPublished(item);
    }

    private SystemAnnouncement active(Long id) {
        return repository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> BusinessException.notFound("通知不存在"));
    }

    private void applyContent(
        SystemAnnouncement item,
        ValidatedCommand value
    ) {
        item.setTitle(value.title());
        item.setContent(value.content());
    }

    private void applyMode(
        SystemAnnouncement item,
        ValidatedCommand value,
        boolean editing
    ) {
        if ("SCHEDULED".equals(value.mode())) {
            item.setStatus("SCHEDULED");
            item.setScheduledAt(value.scheduledAt());
            return;
        }
        if ("DRAFT".equals(value.mode())) {
            item.setStatus("DRAFT");
            item.setScheduledAt(null);
            if (editing) {
                item.setPublishedAt(null);
                item.setAudienceUserIdMax(null);
                item.setRecipientCount(0);
            }
        }
    }

    private void publishEntity(SystemAnnouncement item, AdminUser admin) {
        Long maximumUserId = jdbc.queryForObject(
            "SELECT COALESCE(MAX(id),0) FROM users",
            Long.class
        );
        long audienceMaximum = maximumUserId == null ? 0 : maximumUserId;
        Long recipientCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE id<=?",
            Long.class,
            audienceMaximum
        );
        item.setStatus("PUBLISHED");
        item.setScheduledAt(null);
        item.setPublishedAt(LocalDateTime.now());
        item.setAudienceUserIdMax(audienceMaximum);
        item.setRecipientCount(recipientCount == null ? 0 : recipientCount);
        item.setUpdatedByAdmin(admin);
    }

    private ValidatedCommand validate(SaveCommand command) {
        String mode = command.mode() == null
            ? ""
            : command.mode().trim().toUpperCase();
        if (!SAVE_MODES.contains(mode)) {
            throw BusinessException.badRequest("请选择发布方式");
        }
        LocalDateTime scheduledAt = command.scheduledAt();
        if ("SCHEDULED".equals(mode)) {
            if (scheduledAt == null ||
                !scheduledAt.isAfter(LocalDateTime.now().plusMinutes(1))) {
                throw BusinessException.badRequest("定时发布时间至少晚于当前时间1分钟");
            }
        } else {
            scheduledAt = null;
        }
        return new ValidatedCommand(
            required(command.title(), 120, "请填写通知标题"),
            required(command.content(), 2000, "请填写通知正文"),
            mode,
            scheduledAt
        );
    }

    private String required(String source, int maximum, String message) {
        String value = source == null ? "" : source.trim();
        if (value.isEmpty()) throw BusinessException.badRequest(message);
        if (value.length() > maximum) {
            throw BusinessException.badRequest("内容长度超出限制");
        }
        return value;
    }

    private void notifyPublished(SystemAnnouncement item) {
        realtime.afterCommitToAllUsers(
            "NOTIFICATION_CREATED",
            Map.of(
                "id", item.getId(),
                "sourceType", "ANNOUNCEMENT",
                "title", item.getTitle(),
                "content", item.getContent()
            )
        );
    }

    private void notifyWithdrawn(Long id) {
        realtime.afterCommitToAllUsers(
            "ANNOUNCEMENT_WITHDRAWN",
            Map.of("id", id)
        );
    }

    private void audit(
        AdminUser admin,
        String action,
        SystemAnnouncement item,
        String detail,
        String ipAddress
    ) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUser(admin);
        log.setAction(action);
        log.setTargetType("SYSTEM_ANNOUNCEMENT");
        log.setTargetId(String.valueOf(item.getId()));
        log.setDetail(detail + " | " + item.getTitle());
        log.setIpAddress(ipAddress);
        auditLogs.save(log);
    }

    public record SaveCommand(
        String title,
        String content,
        String mode,
        LocalDateTime scheduledAt
    ) {
    }

    private record ValidatedCommand(
        String title,
        String content,
        String mode,
        LocalDateTime scheduledAt
    ) {
    }

    public record AnnouncementView(
        Long id,
        String title,
        String content,
        String status,
        LocalDateTime scheduledAt,
        LocalDateTime publishedAt,
        long recipientCount,
        String updatedBy,
        LocalDateTime updatedAt
    ) {
        static AnnouncementView from(SystemAnnouncement item) {
            return new AnnouncementView(
                item.getId(),
                item.getTitle(),
                item.getContent(),
                item.getStatus(),
                item.getScheduledAt(),
                item.getPublishedAt(),
                item.getRecipientCount(),
                item.getUpdatedByAdmin().getDisplayName(),
                item.getUpdatedAt()
            );
        }
    }

    public record PageResult(
        List<AnnouncementView> items,
        long total,
        int page,
        int size
    ) {
    }
}
