package com.shixianwen.notification;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.admin.AdminAuthorizationService;
import com.shixianwen.admin.CurrentAdmin;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AdminSystemAnnouncementController {
    private final SystemAnnouncementService service;
    private final AdminAuthorizationService authorization;

    @GetMapping
    public ApiResponse<SystemAnnouncementService.PageResult> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.list(page, size));
    }

    @PostMapping
    public ApiResponse<SystemAnnouncementService.AnnouncementView> create(
        @CurrentAdmin AdminUser admin,
        @Valid @RequestBody SaveRequest request,
        HttpServletRequest servletRequest
    ) {
        requirePublishPermissionForActiveMode(admin, request.mode());
        return ApiResponse.ok(
            service.create(admin, request.command(), ip(servletRequest))
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<SystemAnnouncementService.AnnouncementView> update(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @Valid @RequestBody SaveRequest request,
        HttpServletRequest servletRequest
    ) {
        requirePublishPermissionForActiveMode(admin, request.mode());
        return ApiResponse.ok(
            service.update(admin, id, request.command(), ip(servletRequest))
        );
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<SystemAnnouncementService.AnnouncementView> publish(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(
            service.publish(admin, id, ip(servletRequest))
        );
    }

    @PostMapping("/{id}/withdraw")
    public ApiResponse<SystemAnnouncementService.AnnouncementView> withdraw(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(
            service.withdraw(admin, id, ip(servletRequest))
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        HttpServletRequest servletRequest
    ) {
        service.delete(admin, id, ip(servletRequest));
        return ApiResponse.ok();
    }

    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null
            ? request.getRemoteAddr()
            : forwarded.split(",")[0].trim();
    }

    private void requirePublishPermissionForActiveMode(AdminUser admin, String mode) {
        if (!"DRAFT".equalsIgnoreCase(mode) && !authorization.hasPermission(admin, "ANNOUNCEMENT_PUBLISH")) {
            throw BusinessException.forbidden("没有发布通知的权限");
        }
    }

    public record SaveRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 2000) String content,
        @NotBlank String mode,
        LocalDateTime scheduledAt
    ) {
        SystemAnnouncementService.SaveCommand command() {
            return new SystemAnnouncementService.SaveCommand(
                title,
                content,
                mode,
                scheduledAt
            );
        }
    }
}
