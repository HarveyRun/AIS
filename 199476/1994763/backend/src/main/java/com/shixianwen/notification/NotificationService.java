package com.shixianwen.notification;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;

    public void send(User user, String title, String content, String targetPath) {
        Notification item = new Notification();
        item.setUser(user);
        item.setTitle(title);
        item.setContent(content);
        item.setTargetPath(targetPath);
        repository.save(item);
    }

    public List<NotificationView> list(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(NotificationView::of).toList();
    }

    @Transactional
    public void read(Long userId, Long id) {
        Notification item = repository.findById(id).orElseThrow(() -> BusinessException.notFound("通知不存在"));
        if (!item.getUser().getId().equals(userId)) throw BusinessException.forbidden("无权操作该通知");
        item.setRead(true);
    }

    public record NotificationView(Long id, String title, String content, String targetPath, boolean read,
                                   java.time.LocalDateTime createdAt) {
        static NotificationView of(Notification item) {
            return new NotificationView(item.getId(), item.getTitle(), item.getContent(), item.getTargetPath(),
                    item.isRead(), item.getCreatedAt());
        }
    }
}
