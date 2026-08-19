package com.shixianwen.version;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.admin.CurrentAdmin;
import com.shixianwen.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

@RestController
@RequestMapping("/api/admin/app-versions")
@RequiredArgsConstructor
public class AdminAppVersionController {
    private final AppVersionService service;

    @GetMapping
    public ApiResponse<AppVersionService.PageResult> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.list(page, size));
    }

    @PostMapping
    public ApiResponse<AppVersionService.VersionView> create(
        @CurrentAdmin AdminUser admin,
        @Valid @RequestBody SaveRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.create(admin, request.command(), clientIp(servletRequest)));
    }

    @PutMapping("/{id}")
    public ApiResponse<AppVersionService.VersionView> update(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @Valid @RequestBody SaveRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.update(admin, id, request.command(), clientIp(servletRequest)));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<AppVersionService.VersionView> publish(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.publish(admin, id, clientIp(servletRequest)));
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<AppVersionService.VersionView> unpublish(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.unpublish(admin, id, clientIp(servletRequest)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        HttpServletRequest servletRequest
    ) {
        service.delete(admin, id, clientIp(servletRequest));
        return ApiResponse.ok();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null
            ? request.getRemoteAddr()
            : forwarded.split(",")[0].trim();
    }

    public record SaveRequest(
        @NotBlank String platform,
        @NotBlank @Size(max = 30) String versionName,
        @Min(1) @Max(Integer.MAX_VALUE) int versionCode,
        @Min(1) @Max(Integer.MAX_VALUE) int minimumSupportedVersionCode,
        @NotBlank @Size(max = 80) String title,
        @NotBlank @Size(max = 1000) String updateContent,
        @NotBlank @Size(max = 500) String downloadUrl
    ) {
        AppVersionService.SaveCommand command() {
            return new AppVersionService.SaveCommand(
                platform,
                versionName,
                versionCode,
                minimumSupportedVersionCode,
                title,
                updateContent,
                downloadUrl
            );
        }
    }
}
