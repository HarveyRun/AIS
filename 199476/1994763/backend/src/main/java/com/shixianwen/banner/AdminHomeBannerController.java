package com.shixianwen.banner;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminHomeBannerController {
    private final HomeBannerService service;

    @GetMapping
    public ApiResponse<HomeBannerService.PageResult> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.list(page, size));
    }

    @PostMapping("/images")
    public ApiResponse<HomeBannerService.ImageUploadView> uploadImage(
        @RequestPart("image") MultipartFile image
    ) {
        return ApiResponse.ok(service.uploadImage(image));
    }

    @PostMapping
    public ApiResponse<HomeBannerService.BannerView> create(
        @CurrentAdmin AdminUser admin,
        @Valid @RequestBody SaveRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.create(admin, request.command(), clientIp(servletRequest)));
    }

    @PutMapping("/{id}")
    public ApiResponse<HomeBannerService.BannerView> update(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @Valid @RequestBody SaveRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.update(admin, id, request.command(), clientIp(servletRequest)));
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<HomeBannerService.BannerView> setEnabled(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @Valid @RequestBody EnabledRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(
            service.setEnabled(admin, id, request.enabled(), clientIp(servletRequest))
        );
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
        return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }

    public record SaveRequest(
        @NotBlank String displayMode,
        @Size(max = 30) String labelText,
        @Size(max = 80) String title,
        @Size(max = 200) String description,
        @Size(max = 500) String imageUrl,
        @Min(0) @Max(9999) int sortOrder,
        boolean enabled
    ) {
        HomeBannerService.SaveCommand command() {
            return new HomeBannerService.SaveCommand(
                displayMode,
                labelText,
                title,
                description,
                imageUrl,
                sortOrder,
                enabled
            );
        }
    }

    public record EnabledRequest(boolean enabled) {
    }
}
