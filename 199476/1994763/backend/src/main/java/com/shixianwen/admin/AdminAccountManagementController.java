package com.shixianwen.admin;

import com.shixianwen.common.ApiResponse;
import com.shixianwen.network.ClientNetworkService;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/admin-users")
@RequiredArgsConstructor
public class AdminAccountManagementController {
    private final AdminAccountManagementService service;
    private final ClientNetworkService clientNetworkService;

    @GetMapping
    public ApiResponse<AdminAccountManagementService.PageResult> list(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.users(keyword, safePage(page), safeSize(size)));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(
        @CurrentAdmin AdminUser admin,
        @RequestBody AdminUserRequest body,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(service.createUser(
            admin, body.phone(), body.displayName(), body.password(), body.status(), body.roleIds(), ip(request)
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @RequestBody AdminUserRequest body,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(service.updateUser(
            admin, id, body.phone(), body.displayName(), body.status(), ip(request)
        ));
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @RequestBody IdsRequest body,
        HttpServletRequest request
    ) {
        service.assignUserRoles(admin, id, body.ids(), ip(request));
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        HttpServletRequest request
    ) {
        service.resetPassword(admin, id, ip(request));
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        HttpServletRequest request
    ) {
        service.deleteUser(admin, id, ip(request));
        return ApiResponse.ok();
    }

    private String ip(HttpServletRequest request) {
        return clientNetworkService.resolve(request).ipAddress();
    }

    private int safePage(int value) {
        return Math.max(0, value);
    }

    private int safeSize(int value) {
        return Math.min(100, Math.max(1, value));
    }

    public record AdminUserRequest(
        String phone,
        String displayName,
        String password,
        String status,
        List<Long> roleIds
    ) {
    }

    public record IdsRequest(List<Long> ids) {
    }
}
