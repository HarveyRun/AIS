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

    @PutMapping("/{id}/read")
    public ApiResponse<Void> read(@CurrentUser User user, @PathVariable Long id) {
        service.read(user.getId(), id);
        return ApiResponse.ok();
    }
}
