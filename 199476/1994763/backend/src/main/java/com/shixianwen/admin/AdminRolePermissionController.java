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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRolePermissionController {
    private final AdminAccountManagementService service;
    private final ClientNetworkService clientNetworkService;

    @GetMapping("/roles")
    public ApiResponse<AdminAccountManagementService.PageResult> roles(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.roles(keyword, safePage(page), safeSize(size)));
    }

    @GetMapping("/roles/options")
    public ApiResponse<List<Map<String, Object>>> roleOptions(@CurrentAdmin AdminUser admin) {
        return ApiResponse.ok(service.roleOptions(admin));
    }

    @PostMapping("/roles")
    public ApiResponse<Map<String, Object>> createRole(
        @CurrentAdmin AdminUser admin,
        @RequestBody RoleRequest body,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(service.createRole(
            admin, body.code(), body.name(), body.level(), body.description(), body.active(),
            body.permissionIds(), ip(request)
        ));
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<Map<String, Object>> updateRole(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @RequestBody RoleRequest body,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(service.updateRole(
            admin, id, body.code(), body.name(), body.level(), body.description(), body.active(), ip(request)
        ));
    }

    @PutMapping("/roles/{id}/permissions")
    public ApiResponse<Void> assignPermissions(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @RequestBody IdsRequest body,
        HttpServletRequest request
    ) {
        service.assignRolePermissions(admin, id, body.ids(), ip(request));
        return ApiResponse.ok();
    }

    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> deleteRole(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        HttpServletRequest request
    ) {
        service.deleteRole(admin, id, ip(request));
        return ApiResponse.ok();
    }

    @GetMapping("/permissions")
    public ApiResponse<AdminAccountManagementService.PageResult> permissions(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "") String module,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.permissions(keyword, module, safePage(page), safeSize(size)));
    }

    @GetMapping("/permissions/options")
    public ApiResponse<List<Map<String, Object>>> permissionOptions(@CurrentAdmin AdminUser admin) {
        return ApiResponse.ok(service.permissionOptions(admin));
    }

    @GetMapping("/permissions/modules")
    public ApiResponse<List<String>> permissionModules() {
        return ApiResponse.ok(service.permissionModules());
    }

    @PostMapping("/permissions")
    public ApiResponse<Map<String, Object>> createPermission(
        @CurrentAdmin AdminUser admin,
        @RequestBody PermissionRequest body,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(service.createPermission(
            admin, body.code(), body.name(), body.moduleName(), body.actionName(), body.sortOrder(), body.active(),
            ip(request)
        ));
    }

    @PutMapping("/permissions/{id}")
    public ApiResponse<Map<String, Object>> updatePermission(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @RequestBody PermissionRequest body,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(service.updatePermission(
            admin, id, body.code(), body.name(), body.moduleName(), body.actionName(), body.sortOrder(), body.active(),
            ip(request)
        ));
    }

    @DeleteMapping("/permissions/{id}")
    public ApiResponse<Void> deletePermission(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        HttpServletRequest request
    ) {
        service.deletePermission(admin, id, ip(request));
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

    public record RoleRequest(
        String code,
        String name,
        Integer level,
        String description,
        Boolean active,
        List<Long> permissionIds
    ) {
    }

    public record PermissionRequest(
        String code,
        String name,
        String moduleName,
        String actionName,
        Integer sortOrder,
        Boolean active
    ) {
    }

    public record IdsRequest(List<Long> ids) {
    }
}
