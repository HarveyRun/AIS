package com.shixianwen.notification;

import com.shixianwen.common.BusinessException;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final String PERSONAL = "PERSONAL";
    private static final String ANNOUNCEMENT = "ANNOUNCEMENT";

    private final NotificationRepository repository;
    private final RealtimePublisher realtime;
    private final JdbcTemplate jdbc;

    public void send(User user, String title, String content, String targetPath) {
        Notification item = new Notification();
        item.setUser(user);
        item.setTitle(title);
        item.setContent(content);
        item.setTargetPath(targetPath);
        item = repository.save(item);
        realtime.afterCommit(
            user.getId(),
            "NOTIFICATION_CREATED",
            new NotificationEvent(
                item.getId(),
                PERSONAL,
                title,
                content,
                targetPath
            )
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationView> list(Long userId) {
        List<NotificationView> result = new ArrayList<>();
        result.addAll(
            repository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationView::personal)
                .toList()
        );
        result.addAll(announcementViews(userId));
        result.sort(
            Comparator.comparing(
                NotificationView::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())
            )
        );
        return result;
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        Long announcementUnread = jdbc.queryForObject(
            "SELECT COUNT(*) FROM system_announcements a " +
                "LEFT JOIN system_announcement_reads r " +
                "ON r.announcement_id=a.id AND r.user_id=? " +
                "WHERE a.status='PUBLISHED' AND a.deleted_at IS NULL " +
                "AND a.published_at<=NOW(6) AND ?<=a.audience_user_id_max " +
                "AND r.user_id IS NULL",
            Long.class,
            userId,
            userId
        );
        return repository.countByUserIdAndReadFalse(userId)
            + (announcementUnread == null ? 0 : announcementUnread);
    }

    @Transactional
    public void read(Long userId, String sourceType, Long id) {
        String normalized = sourceType == null
            ? ""
            : sourceType.trim().toUpperCase();
        if (ANNOUNCEMENT.equals(normalized)) {
            readAnnouncement(userId, id);
            return;
        }
        if (!PERSONAL.equals(normalized)) {
            throw BusinessException.badRequest("通知类型不正确");
        }
        Notification item = repository
            .findById(id)
            .orElseThrow(() -> BusinessException.notFound("通知不存在"));
        if (!item.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("无权操作该通知");
        }
        item.setRead(true);
        realtime.afterCommit(
            userId,
            "NOTIFICATION_READ",
            Map.of("id", id, "sourceType", PERSONAL)
        );
    }

    @Transactional
    public void readAll(Long userId) {
        List<Notification> unreadItems = repository
            .findByUserIdAndReadFalse(userId);
        unreadItems.forEach(item -> item.setRead(true));
        jdbc.update(
            "INSERT IGNORE INTO system_announcement_reads " +
                "(announcement_id,user_id,read_at) " +
                "SELECT a.id,?,NOW(6) FROM system_announcements a " +
                "WHERE a.status='PUBLISHED' AND a.deleted_at IS NULL " +
                "AND a.published_at<=NOW(6) AND ?<=a.audience_user_id_max",
            userId,
            userId
        );
        realtime.afterCommit(
            userId,
            "NOTIFICATIONS_READ_ALL",
            Map.of()
        );
    }

    private List<NotificationView> announcementViews(Long userId) {
        return jdbc.query(
            "SELECT a.id,a.title,a.content,a.published_at AS createdAt," +
                "CASE WHEN r.user_id IS NULL THEN FALSE ELSE TRUE END AS readFlag " +
                "FROM system_announcements a " +
                "LEFT JOIN system_announcement_reads r " +
                "ON r.announcement_id=a.id AND r.user_id=? " +
                "WHERE a.status='PUBLISHED' AND a.deleted_at IS NULL " +
                "AND a.published_at<=NOW(6) AND ?<=a.audience_user_id_max",
            (resultSet, rowNumber) -> new NotificationView(
                resultSet.getLong("id"),
                ANNOUNCEMENT,
                resultSet.getString("title"),
                resultSet.getString("content"),
                "",
                resultSet.getBoolean("readFlag"),
                resultSet.getTimestamp("createdAt").toLocalDateTime()
            ),
            userId,
            userId
        );
    }

    private void readAnnouncement(Long userId, Long id) {
        Long visible = jdbc.queryForObject(
            "SELECT COUNT(*) FROM system_announcements " +
                "WHERE id=? AND status='PUBLISHED' AND deleted_at IS NULL " +
                "AND published_at<=NOW(6) AND ?<=audience_user_id_max",
            Long.class,
            id,
            userId
        );
        if (visible == null || visible == 0) {
            throw BusinessException.notFound("通知不存在");
        }
        jdbc.update(
            "INSERT IGNORE INTO system_announcement_reads " +
                "(announcement_id,user_id,read_at) VALUES (?,?,NOW(6))",
            id,
            userId
        );
        realtime.afterCommit(
            userId,
            "NOTIFICATION_READ",
            Map.of("id", id, "sourceType", ANNOUNCEMENT)
        );
    }

    public record NotificationView(
        Long id,
        String sourceType,
        String title,
        String content,
        String targetPath,
        boolean read,
        LocalDateTime createdAt
    ) {
        static NotificationView personal(Notification item) {
            return new NotificationView(
                item.getId(),
                PERSONAL,
                item.getTitle(),
                item.getContent(),
                item.getTargetPath(),
                item.isRead(),
                item.getCreatedAt()
            );
        }
    }

    public record NotificationEvent(
        Long id,
        String sourceType,
        String title,
        String content,
        String targetPath
    ) {
    }
}
