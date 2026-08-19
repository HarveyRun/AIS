package com.shixianwen.notification;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @GetMapping
    public ApiResponse<List<NotificationService.NotificationView>> list(@CurrentUser User user) {
        return ApiResponse.ok(service.list(user.getId()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@CurrentUser User user) {
        return ApiResponse.ok(service.unreadCount(user.getId()));
    }

    @PutMapping("/{sourceType}/{id}/read")
    public ApiResponse<Void> read(
        @CurrentUser User user,
        @PathVariable String sourceType,
        @PathVariable Long id
    ) {
        service.read(user.getId(), sourceType, id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> readPersonal(
        @CurrentUser User user,
        @PathVariable Long id
    ) {
        service.read(user.getId(), "PERSONAL", id);
        return ApiResponse.ok();
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> readAll(@CurrentUser User user) {
        service.readAll(user.getId());
        return ApiResponse.ok();
    }
}
