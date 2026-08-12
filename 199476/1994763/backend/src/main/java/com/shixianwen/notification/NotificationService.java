package com.shixianwen.notification;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import com.shixianwen.realtime.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;
    private final RealtimePublisher realtime;

    public void send(User user, String title, String content, String targetPath) {
        Notification item = new Notification();
        item.setUser(user);
        item.setTitle(title);
        item.setContent(content);
        item.setTargetPath(targetPath);
        item = repository.save(item);
        realtime.afterCommit(user.getId(), "NOTIFICATION_CREATED", new NotificationEvent(
            item.getId(), title, content, targetPath
        ));
    }

    public List<NotificationView> list(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(NotificationView::of).toList();
    }

    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void read(Long userId, Long id) {
        Notification item = repository.findById(id).orElseThrow(() -> BusinessException.notFound("通知不存在"));
        if (!item.getUser().getId().equals(userId)) throw BusinessException.forbidden("无权操作该通知");
        item.setRead(true);
        realtime.afterCommit(userId, "NOTIFICATION_READ", java.util.Map.of("id", id));
    }

    @Transactional
    public void readAll(Long userId) {
        List<Notification> unreadItems = repository.findByUserIdAndReadFalse(userId);
        if (unreadItems.isEmpty()) return;
        unreadItems.forEach(item -> item.setRead(true));
        realtime.afterCommit(userId, "NOTIFICATIONS_READ_ALL", java.util.Map.of());
    }

    public record NotificationView(Long id, String title, String content, String targetPath, boolean read,
                                   java.time.LocalDateTime createdAt) {
        static NotificationView of(Notification item) {
            return new NotificationView(item.getId(), item.getTitle(), item.getContent(), item.getTargetPath(),
                    item.isRead(), item.getCreatedAt());
        }
    }

    public record NotificationEvent(Long id, String title, String content, String targetPath) {}
}
