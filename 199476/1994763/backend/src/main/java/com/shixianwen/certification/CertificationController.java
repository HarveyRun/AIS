package com.shixianwen.certification;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {
    private final CertificationService certificationService;
    private final CertificationPublicMediaService publicMediaService;
    private final ExperienceDraftService experienceDraftService;

    public CertificationController(
        CertificationService certificationService,
        CertificationPublicMediaService publicMediaService,
        ExperienceDraftService experienceDraftService
    ) {
        this.certificationService = certificationService;
        this.publicMediaService = publicMediaService;
        this.experienceDraftService = experienceDraftService;
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
        @RequestParam(required = false) Long experienceCategoryId,
        @RequestParam(defaultValue = "false") boolean removeProofArchive,
        @RequestParam(required = false) String proofUploadId,
        @RequestPart(value = "reviewOriginal", required = false) MultipartFile legacyReviewOriginal,
        @RequestPart(value = "proofArchive", required = false) MultipartFile proofArchive,
        @RequestPart(value = "files", required = false) List<MultipartFile> legacyFiles
    ) {
        if (experienceCategoryId == null) {
            throw BusinessException.badRequest("请选择经历分类");
        }
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
            clientPlatform,
            proofUploadId,
            experienceCategoryId
        ));
    }

    @PostMapping(value = "/experiences", consumes = "application/json")
    public ApiResponse<CertificationService.CertificationView> submitExperienceMetadata(
        @CurrentUser User user,
        @RequestHeader(value = "X-Client-Platform", required = false) String clientPlatform,
        @RequestBody ExperienceSubmission request
    ) {
        if (request.experienceCategoryId() == null) {
            throw BusinessException.badRequest("请选择经历分类");
        }
        return ApiResponse.ok(certificationService.submitExperience(
            user, request.existingId(), request.title(), request.description(),
            request.experienceLocation(), request.experienceStartDate(),
            request.experienceEndDate(), request.experienceCount(),
            request.experienceRole(), request.experienceAgeRange(),
            request.experienceEducation(), request.experienceJob(),
            Boolean.TRUE.equals(request.removeProofArchive()),
            null, null, List.of(), clientPlatform, request.proofUploadId(),
            request.experienceCategoryId()
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

    @GetMapping("/experiences/draft")
    public ApiResponse<ExperienceDraftService.DraftView> experienceDraft(
        @CurrentUser User user,
        @RequestParam(defaultValue = "CREATE") String key
    ) {
        return ApiResponse.ok(experienceDraftService.get(user.getId(), key));
    }

    @GetMapping("/experiences/drafts")
    public ApiResponse<List<ExperienceDraftService.DraftItem>> experienceDrafts(
        @CurrentUser User user
    ) {
        return ApiResponse.ok(experienceDraftService.list(user.getId()));
    }

    @PutMapping("/experiences/draft")
    public ApiResponse<ExperienceDraftService.DraftView> saveExperienceDraft(
        @CurrentUser User user,
        @RequestParam(defaultValue = "CREATE") String key,
        @RequestBody Map<String, Object> content
    ) {
        return ApiResponse.ok(experienceDraftService.save(user.getId(), key, content));
    }

    @DeleteMapping("/experiences/draft")
    public ApiResponse<Void> deleteExperienceDraft(
        @CurrentUser User user,
        @RequestParam(defaultValue = "CREATE") String key
    ) {
        experienceDraftService.delete(user.getId(), key);
        return ApiResponse.ok();
    }

    public record PublicMediaRequest(List<Long> selectedIds) {
    }

    public record ExperienceSubmission(
        Long existingId,
        String title,
        String description,
        String experienceLocation,
        String experienceStartDate,
        String experienceEndDate,
        Integer experienceCount,
        String experienceRole,
        String experienceAgeRange,
        String experienceEducation,
        String experienceJob,
        Long experienceCategoryId,
        Boolean removeProofArchive,
        String proofUploadId
    ) {}

}
