package com.shixianwen.certification;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {
    private final CertificationService certificationService;
    private final String adminKey;

    public CertificationController(
        CertificationService certificationService,
        @Value("${app.admin.key:change-me}") String adminKey
    ) {
        this.certificationService = certificationService;
        this.adminKey = adminKey;
    }

    @GetMapping("/me")
    public ApiResponse<List<CertificationService.CertificationView>> list(@CurrentUser User user) {
        return ApiResponse.ok(certificationService.list(user));
    }

    @PostMapping(value = "/basic/{type}", consumes = "multipart/form-data")
    public ApiResponse<CertificationService.CertificationView> submitBasic(
        @CurrentUser User user,
        @PathVariable String type,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) Integer years,
        @RequestPart("files") List<MultipartFile> files
    ) {
        return ApiResponse.ok(certificationService.submitBasic(user, type, title, years, files));
    }

    @PostMapping(value = "/experiences", consumes = "multipart/form-data")
    public ApiResponse<CertificationService.CertificationView> submitExperience(
        @CurrentUser User user,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) Integer years,
        @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return ApiResponse.ok(certificationService.submitExperience(
            user, title, description, years, files == null ? List.of() : files
        ));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<CertificationService.CertificationView> review(
        @RequestHeader("X-Admin-Key") String providedAdminKey,
        @PathVariable Long id,
        @RequestBody ReviewRequest request
    ) {
        if (!adminKey.equals(providedAdminKey)) {
            throw com.shixianwen.common.BusinessException.forbidden("无权审核认证");
        }
        return ApiResponse.ok(certificationService.review(id, request.approved(), request.reason()));
    }

    public record ReviewRequest(boolean approved, String reason) {
    }
}
