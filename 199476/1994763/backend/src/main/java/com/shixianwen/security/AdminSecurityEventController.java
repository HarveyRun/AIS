package com.shixianwen.security;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.admin.CurrentAdmin;
import com.shixianwen.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/security-events")
@RequiredArgsConstructor
public class AdminSecurityEventController {
    private final SecurityEventService service;

    @GetMapping
    public ApiResponse<SecurityEventService.PageView> list(
        @RequestParam(required = false) String severity,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.list(severity, status, type, page, size));
    }

    @PatchMapping("/{id}/review")
    public ApiResponse<SecurityEventService.View> review(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(service.review(admin, id));
    }
}
