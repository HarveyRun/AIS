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

    @PostMapping(value = "/identity", consumes = "multipart/form-data")
    public ApiResponse<CertificationService.CertificationView> submitIdentity(
        @CurrentUser User user,
        @RequestPart("files") List<MultipartFile> files
    ) {
        return ApiResponse.ok(certificationService.submitIdentity(user, files));
    }

    @PostMapping(value = "/experiences", consumes = "multipart/form-data")
    public ApiResponse<CertificationService.CertificationView> submitExperience(
        @CurrentUser User user,
        @RequestHeader(value = "X-Client-Platform", required = false) String clientPlatform,
        @RequestParam(required = false) Long existingId,
        @RequestParam String title,
        @RequestParam String description,
        @RequestParam(required = false) String experienceLocation,
        @RequestParam(required = false) String experienceStartDate,
        @RequestParam(required = false) String experienceEndDate,
        @RequestParam(required = false) Integer experienceCount,
        @RequestParam(required = false) String experienceRole,
        @RequestParam(required = false) String experienceAgeRange,
        @RequestParam(required = false) String experienceEducation,
        @RequestParam(required = false) String experienceJob,
        @RequestParam(defaultValue = "false") boolean removeProofArchive,
        @RequestPart(value = "reviewOriginal", required = false) MultipartFile legacyReviewOriginal,
        @RequestPart(value = "proofArchive", required = false) MultipartFile proofArchive,
        @RequestPart(value = "files", required = false) List<MultipartFile> legacyFiles
    ) {
        return ApiResponse.ok(certificationService.submitExperience(
            user,
            existingId,
            title,
            description,
            experienceLocation,
            experienceStartDate,
            experienceEndDate,
            experienceCount,
            experienceRole,
            experienceAgeRange,
            experienceEducation,
            experienceJob,
            removeProofArchive,
            legacyReviewOriginal,
            proofArchive,
            legacyFiles == null ? List.of() : legacyFiles,
            clientPlatform
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

    @DeleteMapping("/experiences/{id}")
    public ApiResponse<Void> deleteExperience(
        @CurrentUser User user,
        @PathVariable Long id
    ) {
        certificationService.deleteExperience(user, id);
        return ApiResponse.ok();
    }

    public record PublicMediaRequest(List<Long> selectedIds) {
    }

}
