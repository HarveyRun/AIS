package com.shixianwen.certification;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {
    private final CertificationService certificationService;
    private final CertificationPublicMediaService publicMediaService;

    public CertificationController(
        CertificationService certificationService,
        CertificationPublicMediaService publicMediaService
    ) {
        this.certificationService = certificationService;
        this.publicMediaService = publicMediaService;
    }

    @GetMapping("/me")
    public ApiResponse<List<CertificationService.CertificationView>> list(@CurrentUser User user) {
        return ApiResponse.ok(certificationService.list(user));
    }

    @PostMapping(value = "/basic/IDENTITY", consumes = "multipart/form-data")
    public ApiResponse<CertificationService.CertificationView> submitBasic(
        @CurrentUser User user,
        @RequestPart("files") List<MultipartFile> files
    ) {
        return ApiResponse.ok(certificationService.submitBasic(user, files));
    }

    @PostMapping(value = "/experiences", consumes = "multipart/form-data")
    public ApiResponse<CertificationService.CertificationView> submitExperience(
        @CurrentUser User user,
        @RequestParam(required = false) Long existingId,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam boolean privacyConfirmed,
        @RequestParam String detailMode,
        @RequestPart("signature") MultipartFile signature,
        @RequestPart(value = "reviewOriginal", required = false) MultipartFile reviewOriginal,
        @RequestPart(value = "proofArchive", required = false) MultipartFile proofArchive,
        @RequestPart(value = "detailVideo", required = false) MultipartFile detailVideo,
        @RequestPart(value = "files", required = false) List<MultipartFile> legacyFiles
    ) {
        return ApiResponse.ok(certificationService.submitExperience(
            user,
            existingId,
            title,
            description,
            privacyConfirmed,
            detailMode,
            signature,
            reviewOriginal,
            proofArchive,
            detailVideo,
            legacyFiles == null ? List.of() : legacyFiles
        ));
    }

    @PostMapping(value = "/experiences/public-welfare", consumes = "multipart/form-data")
    public ApiResponse<CertificationService.CertificationView> submitPublicWelfareExperience(
        @CurrentUser User user,
        @RequestParam(required = false) Long existingId,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam String detailMode,
        @RequestPart(value = "proofArchive", required = false) MultipartFile proofArchive,
        @RequestPart(value = "detailVideo", required = false) MultipartFile detailVideo
    ) {
        return ApiResponse.ok(certificationService.submitPublicWelfareExperience(
            user, existingId, title, description, detailMode, proofArchive, detailVideo
        ));
    }

    @PostMapping(value = "/experiences/monetized", consumes = "multipart/form-data")
    public ApiResponse<CertificationService.CertificationView> submitMonetizedExperience(
        @CurrentUser User user,
        @RequestParam(required = false) Long existingId,
        @RequestParam(required = false) Long upgradeSourceId,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam String detailMode,
        @RequestParam boolean privacyConfirmed,
        @RequestPart("signature") MultipartFile signature,
        @RequestPart(value = "reviewOriginal", required = false) MultipartFile reviewOriginal,
        @RequestPart(value = "proofArchive", required = false) MultipartFile proofArchive,
        @RequestPart(value = "detailVideo", required = false) MultipartFile detailVideo
    ) {
        return ApiResponse.ok(certificationService.submitMonetizedExperience(
            user, existingId, upgradeSourceId, title, description, detailMode,
            privacyConfirmed, signature, reviewOriginal,
            proofArchive, detailVideo
        ));
    }

    @GetMapping("/experiences/{id}/public-media")
    public ApiResponse<CertificationPublicMediaService.PublicMediaView> publicMedia(
        @CurrentUser User user,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(publicMediaService.list(user, id));
    }

    @PutMapping("/experiences/{id}/public-media")
    public ApiResponse<CertificationPublicMediaService.PublicMediaView> updatePublicMedia(
        @CurrentUser User user,
        @PathVariable Long id,
        @RequestBody PublicMediaRequest request
    ) {
        return ApiResponse.ok(publicMediaService.update(user, id, request.selectedIds()));
    }

    public record PublicMediaRequest(List<Long> selectedIds) {
    }

}
