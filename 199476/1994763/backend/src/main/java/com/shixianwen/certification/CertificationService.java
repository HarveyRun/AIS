package com.shixianwen.certification;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.common.BusinessException;
import com.shixianwen.config.AppGlobalSettingService;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StoredFile;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.storage.FileTypeDetector;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class CertificationService {
    private static final long TWO_GB = 2L * 1024 * 1024 * 1024;
    private static final long MAX_UNAPPROVED_EXPERIENCES = 3;
    private static final List<String> EXPERIENCE_PROOF_KINDS = List.of(
        "ARCHIVE",
        "PROOF_ARCHIVE",
        "REVIEW_ORIGINAL_ARCHIVE"
    );

    private final CertificationRepository certificationRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;
    private final SensitiveWordService sensitiveWords;
    private final FileTypeDetector fileTypeDetector;
    private final AnalyticsEventService analytics;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired(required = false)
    private AppGlobalSettingService globalSettings;

    public CertificationService(
            CertificationRepository certificationRepository,
            UserRepository userRepository,
            FileStorage fileStorage,
            SensitiveWordService sensitiveWords,
            FileTypeDetector fileTypeDetector,
            AnalyticsEventService analytics) {
        this.certificationRepository = certificationRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
        this.sensitiveWords = sensitiveWords;
        this.fileTypeDetector = fileTypeDetector;
        this.analytics = analytics;
    }

    @Transactional(readOnly = true)
    public List<CertificationView> list(User user) {
        return certificationRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
                .sorted((left, right) -> {
                    int timeComparison = resolveLastOperatedAt(right)
                            .compareTo(resolveLastOperatedAt(left));
                    if (timeComparison != 0) return timeComparison;
                    return right.getId().compareTo(left.getId());
                })
                .map(this::view)
                .toList();
    }

    @Transactional
    public CertificationView submitIdentity(User user, List<MultipartFile> files) {
        user = lockedUser(user);
        List<MultipartFile> supplied = files == null
            ? List.of()
            : files.stream().filter(this::present).toList();
        if (supplied.size() != 3) {
            throw BusinessException.badRequest("请拍摄身份证正面、反面和手持身份证照片");
        }
        if (supplied.stream().anyMatch(file -> !"IMAGE".equals(fileTypeDetector.detect(file).kind()))) {
            throw BusinessException.badRequest("实名认证材料仅支持现场拍摄的图片");
        }

        Certification certification = certificationRepository
            .findFirstByUserIdAndCertificationTypeOrderByIdDesc(user.getId(), "IDENTITY")
            .orElse(null);
        if (certification != null && !"REJECTED".equals(certification.getStatus())) {
            throw BusinessException.badRequest(
                "APPROVED".equals(certification.getStatus())
                    ? "实名认证已经通过"
                    : "实名认证正在审核中"
            );
        }
        if (certification == null) {
            certification = baseCertification(user, "BASIC", "IDENTITY", "实名认证");
        } else {
            certification.setStatus("PENDING");
            certification.setRejectionReason(null);
            certification.setSubmittedAt(LocalDateTime.now());
            certification.setReviewedAt(null);
            LocalDateTime now = LocalDateTime.now();
            certification.getMaterials().stream()
                .filter(material -> material.getDeletedAt() == null)
                .forEach(material -> material.setDeletedAt(now));
        }
        certification.setEnabled(true);
        certification.setTitle("实名认证");
        List<String> kinds = List.of("IDENTITY_FRONT", "IDENTITY_BACK", "IDENTITY_HANDHELD");
        for (int index = 0; index < supplied.size(); index++) {
            attachFile(certification, supplied.get(index), kinds.get(index));
        }
        certification = certificationRepository.save(certification);
        analytics.recordBusinessAfterCommit(user, "identity_submitted", analytics.properties(
            "certification_id", certification.getId(),
            "material_count", supplied.size()
        ));
        return view(certification);
    }

    @Transactional
    public CertificationView submitExperience(
            User user,
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
            boolean removeProofArchive,
            MultipartFile legacyReviewOriginal,
            MultipartFile proofArchive,
            List<MultipartFile> legacyFiles) {
        return submitExperience(
            user, existingId, title, description, experienceLocation, experienceStartDate,
            experienceEndDate, experienceCount, experienceRole, experienceAgeRange,
            experienceEducation, experienceJob, removeProofArchive, legacyReviewOriginal,
            proofArchive, legacyFiles, "ANDROID"
        );
    }

    @Transactional
    public CertificationView submitExperience(
            User user,
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
            boolean removeProofArchive,
            MultipartFile legacyReviewOriginal,
            MultipartFile proofArchive,
            List<MultipartFile> legacyFiles,
            String clientPlatform) {
        user = lockedUser(user);
        if (globalSettings != null && !globalSettings.current().experiencePublishEnabled()) {
            throw BusinessException.badRequest("平台暂时关闭了经历发布");
        }
        Certification certification = experienceForSubmission(user, existingId, title);
        validateExperienceContent(title, description);
        validateAdditionalInformation(
            experienceLocation,
            experienceStartDate,
            experienceEndDate,
            experienceCount,
            experienceRole,
            experienceAgeRange,
            experienceEducation,
            experienceJob
        );
        String cleanDescription = description.trim();
        MultipartFile normalizedProof = normalizedProofArchive(
            proofArchive,
            legacyReviewOriginal,
            legacyFiles
        );
        if (removeProofArchive && present(normalizedProof)) {
            throw BusinessException.badRequest("移除和上传证明资料不能同时操作");
        }
        validateArchive(normalizedProof);
        prepareResubmission(certification, existingId);
        certification.setTitle(sensitiveWords.mask(title.trim()));
        certification.setDescription(sensitiveWords.mask(cleanDescription));
        certification.setExperienceLocation(maskOptional(experienceLocation));
        certification.setExperienceStartDate(maskOptional(experienceStartDate));
        certification.setExperienceEndDate(maskOptional(experienceEndDate));
        certification.setExperienceCount(experienceCount);
        certification.setExperienceRole(maskOptional(experienceRole));
        certification.setExperienceAgeRange(blankToNull(experienceAgeRange));
        certification.setExperienceEducation(blankToNull(experienceEducation));
        certification.setExperienceJob(maskOptional(experienceJob));
        certification.setSourceClientPlatform(
            "IOS".equalsIgnoreCase(clientPlatform == null ? "" : clientPlatform.trim())
                ? "IOS"
                : "ANDROID"
        );
        if (removeProofArchive || present(normalizedProof)) {
            for (String kind : EXPERIENCE_PROOF_KINDS) {
                retireMaterialKind(certification, kind);
            }
        }
        if (present(normalizedProof)) {
            attachFile(certification, normalizedProof, "PROOF_ARCHIVE");
        }
        retireMaterialKind(certification, "SIGNATURE");
        certification.setPrivacyConfirmedAt(null);
        certification = certificationRepository.save(certification);
        recordExperienceSubmitted(user, certification, existingId != null);
        return view(certification);
    }

    public CertificationView submitExperience(
            User user,
            Long existingId,
            String title,
            String description,
            boolean removeProofArchive,
            MultipartFile legacyReviewOriginal,
            MultipartFile proofArchive,
            List<MultipartFile> legacyFiles) {
        return submitExperience(
            user, existingId, title, description,
            null, null, null, null, null, null, null, null,
            removeProofArchive, legacyReviewOriginal, proofArchive, legacyFiles
        );
    }

    private void validateAdditionalInformation(
        String location,
        String startDate,
        String endDate,
        Integer count,
        String role,
        String ageRange,
        String education,
        String job
    ) {
        validateOptionalLength(location, 20, "发生地点最多20个字");
        validateOptionalLength(startDate, 20, "开始时间最多20个字");
        validateOptionalLength(endDate, 20, "结束时间最多20个字");
        validateOptionalLength(role, 7, "本人当时的身份最多7个字");
        validateOptionalLength(job, 12, "当时职业最多12个字");
        if (count != null && (count < 1 || count > 99)) {
            throw BusinessException.badRequest("已经历的次数只能填写1至99");
        }
        if (!blank(ageRange) && !Set.of(
            "0～5岁", "6～14岁", "15～23岁", "24～33岁", "34～54岁", "55岁及以上"
        ).contains(ageRange.trim())) {
            throw BusinessException.badRequest("当时年龄段不正确");
        }
        if (!blank(education) && !Set.of(
            "小学及以下", "初中", "高中/中专", "大专", "本科", "硕士", "博士及以上"
        ).contains(education.trim())) {
            throw BusinessException.badRequest("当时学历不正确");
        }
    }

    private void validateOptionalLength(String value, int max, String message) {
        if (!blank(value) && value.trim().codePointCount(0, value.trim().length()) > max) {
            throw BusinessException.badRequest(message);
        }
    }

    private String maskOptional(String value) {
        return blank(value) ? null : sensitiveWords.mask(value.trim());
    }

    private String blankToNull(String value) {
        return blank(value) ? null : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private User lockedUser(User user) {
        user = userRepository.findWithLockById(user.getId())
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        return user;
    }

    private Certification experienceForSubmission(
        User user,
        Long existingId,
        String title
    ) {
        Certification certification;
        if (existingId == null) {
            long unapprovedExperiences = certificationRepository
                    .countByUserIdAndCategoryAndStatusNot(
                            user.getId(),
                            "EXPERIENCE",
                            "APPROVED");
            long maxUnapproved = globalSettings == null
                ? MAX_UNAPPROVED_EXPERIENCES
                : globalSettings.current().maxUnapprovedExperiences();
            if (unapprovedExperiences >= maxUnapproved) {
                throw BusinessException.badRequest(
                        "添加已达上限，请等待审核完成后再添加");
            }
            certification = baseCertification(
                user,
                "EXPERIENCE",
                "EXPERIENCE",
                title == null ? "" : sensitiveWords.mask(title.trim())
            );
        } else {
            certification = certificationRepository.findByIdAndUserId(existingId, user.getId())
                    .filter(item -> "EXPERIENCE".equals(item.getCategory()))
                    .filter(item -> "REJECTED".equals(item.getStatus()))
                    .orElseThrow(() -> BusinessException.badRequest("只有已驳回的经历才能修改"));
        }
        return certification;
    }

    private void validateExperienceContent(String title, String description) {
        if (title == null || title.isBlank())
            throw BusinessException.badRequest("请填写经历标题");
        int titleLimit = globalSettings == null ? 18 : globalSettings.current().experienceTitleMaxLength();
        if (title.trim().codePointCount(0, title.trim().length()) > titleLimit)
            throw BusinessException.badRequest("经历标题最多" + titleLimit + "个字");
        String cleanDescription = description == null ? "" : description.trim();
        if (cleanDescription.isEmpty())
            throw BusinessException.badRequest("请填写文字详述");
        int descriptionLimit = globalSettings == null ? 400 : globalSettings.current().experienceDescriptionMaxLength();
        if (cleanDescription.codePointCount(0, cleanDescription.length()) > descriptionLimit)
            throw BusinessException.badRequest("文字详述最多" + descriptionLimit + "个字");
    }

    private void prepareResubmission(Certification certification, Long existingId) {
        if (existingId != null) {
            certification.setStatus("PENDING");
            certification.setRejectionReason(null);
            certification.setSubmittedAt(LocalDateTime.now());
        }
    }

    private void recordExperienceSubmitted(
        User user,
        Certification certification,
        boolean resubmitted
    ) {
        analytics.recordBusinessAfterCommit(user, "experience_submitted", analytics.properties(
            "certification_id", certification.getId(),
            "material_count", certification.getMaterials().stream()
                .filter(material -> material.getDeletedAt() == null)
                .count(),
            "resubmitted", resubmitted
        ));
    }

    @Transactional
    public void deleteExperience(User user, Long certificationId) {
        Certification certification = certificationRepository
            .findByIdAndUserId(certificationId, user.getId())
            .filter(item -> "EXPERIENCE".equals(item.getCategory()))
            .orElseThrow(() -> BusinessException.notFound("经历不存在"));
        if (!"REJECTED".equals(certification.getStatus())) {
            throw BusinessException.badRequest("只有已被驳回的经历才能删除");
        }

        LocalDateTime now = LocalDateTime.now();
        certification.setEnabled(false);
        certification.setDeletedAt(now);
        certification.getMaterials().stream()
            .filter(material -> material.getDeletedAt() == null)
            .forEach(material -> material.setDeletedAt(now));
        certificationRepository.save(certification);
        analytics.recordBusinessAfterCommit(user, "experience_deleted", analytics.properties(
            "certification_id", certification.getId(),
            "status", certification.getStatus()
        ));
    }

    @Transactional
    public CertificationView review(Long id, boolean approved, String reason) {
        Certification certification = certificationRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("认证不存在"));
        boolean firstApproval = approved && !"APPROVED".equals(certification.getStatus());
        certification.setStatus(approved ? "APPROVED" : "REJECTED");
        certification.setRejectionReason(approved ? null : reason);
        certification.setReviewedAt(LocalDateTime.now());
        boolean hasProofArchive = "EXPERIENCE".equals(certification.getCategory())
            && (hasMaterial(certification, "PROOF_ARCHIVE") || hasMaterial(certification, "ARCHIVE"));
        if ("EXPERIENCE".equals(certification.getCategory())) {
            certification.setMediaProcessingStatus(
                approved && hasProofArchive ? "PENDING" : "NOT_REQUIRED"
            );
            certification.setMediaProcessingError(null);
            certification.setMediaProcessedAt(null);
        }
        certification = certificationRepository.save(certification);
        refreshAnswererStatus(certification.getUser(), certification.getCertificationType());
        analytics.recordBusinessAfterCommit(
            certification.getUser(),
            approved ? "certification_approved" : "certification_rejected",
            analytics.properties(
                "certification_id", certification.getId(),
                "certification_type", certification.getCertificationType(),
                "category", certification.getCategory()
            )
        );
        if (approved && "EXPERIENCE".equals(certification.getCategory()) && events != null) {
            if (firstApproval) {
                events.publishEvent(new ExperienceApproved(certification.getId()));
            }
            if (hasProofArchive) {
                events.publishEvent(new CertificationMediaExtractionRequested(certification.getId()));
            }
        }
        return view(certification);
    }

    private Certification baseCertification(User user, String category, String type, String title) {
        Certification certification = new Certification();
        certification.setUser(user);
        certification.setCategory(category);
        certification.setCertificationType(type);
        certification.setTitle(title);
        certification.setStatus("PENDING");
        certification.setSubmittedAt(LocalDateTime.now());
        return certification;
    }

    private void attachFile(
        Certification certification,
        MultipartFile file,
        String materialKind
    ) {
        StoredFile stored = fileStorage.store(
            file,
            storagePrefix(certification.getUser()) + "certifications/" + certification.getUser().getUid(),
            StorageVisibility.PRIVATE
        );
        CertificationMaterial material = new CertificationMaterial();
        material.setCertification(certification);
        material.setMaterialKind(materialKind);
        material.setOriginalName(file.getOriginalFilename() == null ? "材料" : file.getOriginalFilename());
        material.setStorageKey(stored.storageKey());
        material.setPublicUrl(null);
        material.setContentType(stored.contentType());
        material.setFileSize(stored.size());
        certification.getMaterials().add(material);
    }

    private String storagePrefix(User user) {
        return "TEST".equals(user.getAccountType()) ? "test/" : "";
    }

    private CertificationView view(Certification certification) {
        return CertificationView.from(certification, fileStorage);
    }

    private static LocalDateTime resolveLastOperatedAt(Certification certification) {
        LocalDateTime result = certification.getCreatedAt();
        for (LocalDateTime candidate : List.of(
                certification.getSubmittedAt() == null ? LocalDateTime.MIN : certification.getSubmittedAt(),
                certification.getReviewedAt() == null ? LocalDateTime.MIN : certification.getReviewedAt(),
                certification.getMediaProcessedAt() == null ? LocalDateTime.MIN : certification.getMediaProcessedAt(),
                certification.getUpdatedAt() == null ? LocalDateTime.MIN : certification.getUpdatedAt())) {
            if (result == null || candidate.isAfter(result)) result = candidate;
        }
        return result == null ? LocalDateTime.MIN : result;
    }

    private void retireMaterialKind(Certification certification, String kind) {
        LocalDateTime now = LocalDateTime.now();
        certification.getMaterials().stream()
            .filter(material -> material.getDeletedAt() == null)
            .filter(material -> kind.equals(material.getMaterialKind()))
            .forEach(material -> material.setDeletedAt(now));
    }

    private MultipartFile normalizedProofArchive(
        MultipartFile proofArchive,
        MultipartFile legacyReviewOriginal,
        List<MultipartFile> legacyFiles
    ) {
        List<MultipartFile> candidates = new java.util.ArrayList<>();
        if (present(proofArchive)) candidates.add(proofArchive);
        if (present(legacyReviewOriginal)) candidates.add(legacyReviewOriginal);
        if (legacyFiles != null) {
            candidates.addAll(legacyFiles.stream().filter(this::present).toList());
        }
        if (candidates.size() > 1) {
            throw BusinessException.badRequest("证明资料只能上传一个压缩包");
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private boolean present(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private void validateArchive(MultipartFile archive) {
        if (!present(archive)) return;
        fileTypeDetector.requireArchive(archive);
        long maxBytes = globalSettings == null ? TWO_GB : globalSettings.current().proofArchiveMaxBytes();
        if (archive.getSize() > maxBytes) {
            throw BusinessException.badRequest("证明资料不能超过" + readableSize(maxBytes));
        }
    }

    private String readableSize(long bytes) {
        long megabytes = Math.max(1, bytes / (1024 * 1024));
        return megabytes >= 1024 && megabytes % 1024 == 0
            ? (megabytes / 1024) + "GB"
            : megabytes + "MB";
    }

    private boolean hasMaterial(Certification certification, String kind) {
        return certification.getMaterials().stream()
            .anyMatch(material -> material.getDeletedAt() == null
                && kind.equals(material.getMaterialKind()));
    }

    private void refreshAnswererStatus(User user, String reviewedCertificationType) {
        List<Certification> certifications = certificationRepository
                .findByUserIdAndStatusAndEnabledTrueOrderByIdAsc(user.getId(), "APPROVED");
        boolean experience = certifications.stream().anyMatch(item ->
            "EXPERIENCE".equals(item.getCategory())
        );
        boolean answererQualified = experience;
        boolean reviewedQualification = "EXPERIENCE".equals(reviewedCertificationType);

        user.setAnswererStatus(answererQualified ? "APPROVED" : "PENDING");
        if (!answererQualified || !"ACTIVE".equals(user.getAccountStatus())) {
            user.setAcceptingInquiries(false);
        } else if (reviewedQualification && user.getInquiryPriceUpdatedAt() != null) {
            user.setAcceptingInquiries(true);
        }
        userRepository.save(user);
    }

    private static List<CertificationMaterial> visibleMaterials(Certification certification) {
        List<CertificationMaterial> active = certification.getMaterials().stream()
            .filter(material -> material.getDeletedAt() == null)
            .toList();
        CertificationMaterial proof = active.stream()
            .filter(material -> List.of("ARCHIVE", "PROOF_ARCHIVE")
                .contains(material.getMaterialKind()))
            .reduce((first, second) -> second)
            .orElseGet(() -> active.stream()
                .filter(material -> "REVIEW_ORIGINAL_ARCHIVE".equals(material.getMaterialKind()))
                .reduce((first, second) -> second)
                .orElse(null));
        return active.stream()
            .filter(material -> !"DETAIL_VIDEO".equals(material.getMaterialKind()))
            .filter(material -> !EXPERIENCE_PROOF_KINDS.contains(material.getMaterialKind())
                || material == proof)
            .toList();
    }

    public record MaterialView(Long id, String kind, String name, String url, long size, String contentType) {
        static MaterialView from(
            CertificationMaterial material,
            FileStorage fileStorage,
            boolean certificationApproved
        ) {
            boolean proofDownloadAllowed = certificationApproved
                || !EXPERIENCE_PROOF_KINDS.contains(material.getMaterialKind());
            String legacyUrl = material.getPublicUrl();
            String url = proofDownloadAllowed
                ? legacyUrl == null || legacyUrl.isBlank()
                    ? fileStorage.accessUrl(material.getStorageKey(), StorageVisibility.PRIVATE)
                    : legacyUrl
                : "";
            return new MaterialView(
                    material.getId(),
                    EXPERIENCE_PROOF_KINDS.contains(material.getMaterialKind())
                        ? "PROOF_ARCHIVE"
                        : material.getMaterialKind(),
                    material.getOriginalName(),
                    url,
                    material.getFileSize(), material.getContentType());
        }
    }

    public record CertificationView(
            Long id,
            String category,
            String type,
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
            String status,
            boolean enabled,
            String rejectionReason,
            String mediaProcessingStatus,
            String mediaProcessingError,
            LocalDateTime lastOperatedAt,
            List<MaterialView> materials) {
        static CertificationView from(Certification certification, FileStorage fileStorage) {
            return new CertificationView(
                    certification.getId(), certification.getCategory(), certification.getCertificationType(),
                    certification.getTitle(), certification.getDescription(),
                    certification.getExperienceLocation(),
                    certification.getExperienceStartDate(),
                    certification.getExperienceEndDate(),
                    certification.getExperienceCount(),
                    certification.getExperienceRole(),
                    certification.getExperienceAgeRange(),
                    certification.getExperienceEducation(),
                    certification.getExperienceJob(),
                    certification.getStatus(), certification.isEnabled(),
                    certification.getRejectionReason(),
                    certification.getMediaProcessingStatus(),
                    certification.getMediaProcessingError(),
                    resolveLastOperatedAt(certification),
                    visibleMaterials(certification).stream()
                            .map(material -> MaterialView.from(
                                material,
                                fileStorage,
                                "APPROVED".equals(certification.getStatus())
                            )).toList());
        }
    }
}
