package com.shixianwen.certification;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.common.BusinessException;
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

@Service
public class CertificationService {
    private static final long ONE_GB = 1024L * 1024 * 1024;
    private static final long TWO_GB = 2L * 1024 * 1024 * 1024;
    private static final long MAX_UNAPPROVED_EXPERIENCES = 3;

    private final CertificationRepository certificationRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;
    private final SensitiveWordService sensitiveWords;
    private final FileTypeDetector fileTypeDetector;
    private final AnalyticsEventService analytics;

    @Autowired
    private ApplicationEventPublisher events;

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
    public CertificationView submitBasic(
            User user,
            List<MultipartFile> files) {
        String normalizedType = "IDENTITY";
        if (files.size() != 3 || files.stream().anyMatch(file -> !isImage(file))) {
            throw BusinessException.badRequest("身份信息认证需要按要求提交3张图片");
        }
        Certification existing = certificationRepository
                .findFirstByUserIdAndCertificationTypeOrderByIdDesc(user.getId(), normalizedType)
                .orElse(null);
        if (existing != null && !"REJECTED".equals(existing.getStatus())) {
            throw BusinessException.badRequest("该基础信息认证已经提交");
        }
        Certification certification = existing == null
                ? baseCertification(
                        user,
                        "BASIC",
                        normalizedType,
                        "实名认证",
                        true)
                : existing;
        certification.setTitle("实名认证");
        certification.setStatus("PENDING");
        certification.setRejectionReason(null);
        certification.setSubmittedAt(LocalDateTime.now());
        retireMaterials(certification);
        attachFiles(certification, files);
        certification = certificationRepository.save(certification);
        analytics.recordBusinessAfterCommit(user, "identity_submitted", analytics.properties(
                "certification_id", certification.getId(),
                "certification_type", normalizedType,
                "material_count", files.size()
            ));
        return view(certification);
    }

    @Transactional
    public CertificationView submitExperience(
            User user,
            Long existingId,
            String title,
            String description,
            boolean privacyConfirmed,
            String detailMode,
            MultipartFile signature,
            MultipartFile reviewOriginal,
            MultipartFile proofArchive,
            MultipartFile detailVideo,
            List<MultipartFile> legacyFiles) {
        MultipartFile normalizedProof = proofArchive;
        if ((normalizedProof == null || normalizedProof.isEmpty()) && legacyFiles.size() == 1) {
            normalizedProof = legacyFiles.get(0);
        }
        return submitMonetizedExperience(
            user, existingId, null, title, description, detailMode,
            privacyConfirmed, signature, reviewOriginal, normalizedProof, detailVideo
        );
    }

    @Transactional
    public CertificationView submitPublicWelfareExperience(
            User user,
            Long existingId,
            String title,
            String description,
            String detailMode,
            MultipartFile proofArchive,
            MultipartFile detailVideo) {
        user = lockedUser(user);
        Certification certification = experienceForSubmission(
            user, existingId, "PUBLIC_WELFARE", title, null
        );
        validateExperienceContent(title, description);
        String cleanDescription = description == null ? "" : description.trim();
        validateDetail(cleanDescription, detailMode, detailVideo, certification);
        validateArchive(
            proofArchive,
            hasMaterial(certification, "PROOF_ARCHIVE") || hasMaterial(certification, "ARCHIVE"),
            "请上传证明资料压缩包"
        );
        prepareResubmission(certification, existingId);
        applyExperienceContent(certification, title, cleanDescription, detailVideo, proofArchive);
        certification.setExperienceBusinessType("PUBLIC_WELFARE");
        certification.setUpgradeSourceId(null);
        certification.setPrivacyConfirmedAt(null);
        retireMaterialKind(certification, "REVIEW_ORIGINAL_ARCHIVE");
        retireMaterialKind(certification, "SIGNATURE");
        certification = certificationRepository.save(certification);
        recordExperienceSubmitted(user, certification, existingId != null, "PUBLIC_WELFARE");
        return view(certification);
    }

    @Transactional
    public CertificationView submitMonetizedExperience(
            User user,
            Long existingId,
            Long upgradeSourceId,
            String title,
            String description,
            String detailMode,
            boolean privacyConfirmed,
            MultipartFile signature,
            MultipartFile reviewOriginal,
            MultipartFile proofArchive,
            MultipartFile detailVideo) {
        user = lockedUser(user);
        requireIdentity(user.getId());
        Certification upgradeSource = requireUpgradeSource(user.getId(), existingId, upgradeSourceId);
        Certification certification = experienceForSubmission(
            user, existingId, "MONETIZED", title, upgradeSourceId
        );
        if (upgradeSource != null && existingId == null) {
            copyMaterialIfMissing(upgradeSource, certification, "DETAIL_VIDEO");
            copyMaterialIfMissing(upgradeSource, certification, "PROOF_ARCHIVE");
            copyMaterialIfMissing(upgradeSource, certification, "ARCHIVE");
        }
        validateExperienceContent(title, description);
        String cleanDescription = description == null ? "" : description.trim();
        validateDetail(cleanDescription, detailMode, detailVideo, certification);
        validateExperienceArchives(reviewOriginal, proofArchive, List.of(), certification);
        if (!privacyConfirmed) {
            throw BusinessException.badRequest("请确认已处理隐私信息");
        }
        validateSignature(signature);
        prepareResubmission(certification, existingId);
        applyExperienceContent(certification, title, cleanDescription, detailVideo, null);
        if (reviewOriginal != null && !reviewOriginal.isEmpty()) {
            retireMaterialKind(certification, "REVIEW_ORIGINAL_ARCHIVE");
            attachFile(certification, reviewOriginal, "REVIEW_ORIGINAL_ARCHIVE");
        }
        if (proofArchive != null && !proofArchive.isEmpty()) {
            retireMaterialKind(certification, "ARCHIVE");
            retireMaterialKind(certification, "PROOF_ARCHIVE");
            attachFile(certification, proofArchive, "PROOF_ARCHIVE");
        }
        retireMaterialKind(certification, "SIGNATURE");
        attachFile(certification, signature, "SIGNATURE");
        certification.setExperienceBusinessType("MONETIZED");
        certification.setUpgradeSourceId(upgradeSourceId);
        certification.setPrivacyConfirmedAt(LocalDateTime.now());
        certification = certificationRepository.save(certification);
        recordExperienceSubmitted(user, certification, existingId != null, "MONETIZED");
        return view(certification);
    }

    private User lockedUser(User user) {
        user = userRepository.findWithLockById(user.getId())
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        return user;
    }

    private void requireIdentity(Long userId) {
        if (!certificationRepository.existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            userId, "IDENTITY", "APPROVED")) {
            throw BusinessException.badRequest("完成实名认证后才能发布干货变现经历");
        }
    }

    private Certification experienceForSubmission(
        User user,
        Long existingId,
        String businessType,
        String title,
        Long upgradeSourceId
    ) {
        Certification certification;
        if (existingId == null) {
            long unapprovedExperiences = certificationRepository
                    .countByUserIdAndCategoryAndStatusNot(
                            user.getId(),
                            "EXPERIENCE",
                            "APPROVED");
            if (unapprovedExperiences >= MAX_UNAPPROVED_EXPERIENCES) {
                throw BusinessException.badRequest(
                        "添加已达上限，请等待审核完成后再添加");
            }
            certification = baseCertification(
                user,
                "EXPERIENCE",
                "EXPERIENCE",
                title == null ? "" : sensitiveWords.mask(title.trim()),
                false
            );
        } else {
            certification = certificationRepository.findByIdAndUserId(existingId, user.getId())
                    .filter(item -> "EXPERIENCE".equals(item.getCategory()))
                    .filter(item -> "REJECTED".equals(item.getStatus()))
                    .orElseThrow(() -> BusinessException.badRequest("只有已驳回的经历才能修改"));
            String existingType = normalizedBusinessType(certification);
            if (!businessType.equals(existingType)) {
                throw BusinessException.badRequest("经历发布类型不能变更");
            }
        }
        certification.setExperienceBusinessType(businessType);
        certification.setUpgradeSourceId(upgradeSourceId);
        return certification;
    }

    private void validateExperienceContent(String title, String description) {
        if (title == null || title.isBlank())
            throw BusinessException.badRequest("请填写经历标题");
        if (title.trim().length() > 20)
            throw BusinessException.badRequest("经历标题最多20个字");
        String cleanDescription = description == null ? "" : description.trim();
        if (cleanDescription.length() > 5000)
            throw BusinessException.badRequest("经历详述最多5000个字");
    }

    private void prepareResubmission(Certification certification, Long existingId) {
        if (existingId != null) {
            certification.setStatus("PENDING");
            certification.setRejectionReason(null);
            certification.setSubmittedAt(LocalDateTime.now());
        }
    }

    private void applyExperienceContent(
        Certification certification,
        String title,
        String cleanDescription,
        MultipartFile detailVideo,
        MultipartFile proofArchive
    ) {
        certification.setTitle(sensitiveWords.mask(title.trim()));
        certification.setDescription(cleanDescription.isBlank() ? null : sensitiveWords.mask(cleanDescription));
        if (detailVideo != null && !detailVideo.isEmpty()) {
            retireMaterialKind(certification, "DETAIL_VIDEO");
            attachFile(certification, detailVideo, "DETAIL_VIDEO");
        }
        if (proofArchive != null && !proofArchive.isEmpty()) {
            retireMaterialKind(certification, "ARCHIVE");
            retireMaterialKind(certification, "PROOF_ARCHIVE");
            attachFile(certification, proofArchive, "PROOF_ARCHIVE");
        }
    }

    private void recordExperienceSubmitted(
        User user,
        Certification certification,
        boolean resubmitted,
        String businessType
    ) {
        analytics.recordBusinessAfterCommit(user, "experience_submitted", analytics.properties(
            "certification_id", certification.getId(),
            "material_count", certification.getMaterials().stream()
                .filter(material -> material.getDeletedAt() == null)
                .count(),
            "business_type", businessType,
            "resubmitted", resubmitted
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
        if ("EXPERIENCE".equals(certification.getCategory())) {
            certification.setMediaProcessingStatus(approved ? "PENDING" : "NOT_REQUIRED");
            certification.setMediaProcessingError(null);
            certification.setMediaProcessedAt(null);
        }
        certification = certificationRepository.save(certification);
        if (approved
            && "MONETIZED".equals(normalizedBusinessType(certification))
            && certification.getUpgradeSourceId() != null) {
            certificationRepository.findByIdAndUserId(
                certification.getUpgradeSourceId(), certification.getUser().getId()
            ).ifPresent(source -> {
                if ("PUBLIC_WELFARE".equals(normalizedBusinessType(source))) {
                    source.setEnabled(false);
                    certificationRepository.save(source);
                }
            });
        }
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
            events.publishEvent(new CertificationMediaExtractionRequested(certification.getId()));
        }
        return view(certification);
    }

    private Certification baseCertification(User user, String category, String type, String title, boolean required) {
        Certification certification = new Certification();
        certification.setUser(user);
        certification.setCategory(category);
        certification.setCertificationType(type);
        certification.setTitle(title);
        certification.setRequiredItem(required);
        certification.setStatus("PENDING");
        certification.setSubmittedAt(LocalDateTime.now());
        return certification;
    }

    private void attachFiles(Certification certification, List<MultipartFile> files) {
        for (MultipartFile file : files) {
            attachFile(certification, file, kindOf(file));
        }
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

    private void retireMaterials(Certification certification) {
        LocalDateTime now = LocalDateTime.now();
        certification.getMaterials().forEach(material -> material.setDeletedAt(now));
    }

    private void retireMaterialKind(Certification certification, String kind) {
        LocalDateTime now = LocalDateTime.now();
        certification.getMaterials().stream()
            .filter(material -> material.getDeletedAt() == null)
            .filter(material -> kind.equals(material.getMaterialKind()))
            .forEach(material -> material.setDeletedAt(now));
    }

    private void validateDetail(
        String description,
        String detailMode,
        MultipartFile detailVideo,
        Certification certification
    ) {
        String normalizedMode = detailMode == null ? "" : detailMode.trim().toUpperCase();
        if (!List.of("TEXT", "VIDEO", "BOTH").contains(normalizedMode)) {
            throw BusinessException.badRequest("请选择文字详述或录像详述");
        }
        boolean hasText = !description.isBlank();
        boolean hasNewVideo = detailVideo != null && !detailVideo.isEmpty();
        boolean hasExistingVideo = certification.getMaterials().stream()
            .anyMatch(material -> material.getDeletedAt() == null
                && "DETAIL_VIDEO".equals(material.getMaterialKind()));
        boolean hasVideo = hasNewVideo || hasExistingVideo;
        if (!hasText && !hasVideo) {
            throw BusinessException.badRequest("请填写文字详述或选择详述录像");
        }
        if ("TEXT".equals(normalizedMode) && !hasText) {
            throw BusinessException.badRequest("请填写文字详述");
        }
        if ("VIDEO".equals(normalizedMode) && !hasVideo) {
            throw BusinessException.badRequest("请选择详述录像");
        }
        if ("BOTH".equals(normalizedMode) && (!hasText || !hasVideo)) {
            throw BusinessException.badRequest("请同时填写文字详述并选择详述录像");
        }
        if (hasNewVideo) {
            if (!isVideo(detailVideo)) {
                throw BusinessException.badRequest("详述录像格式不正确");
            }
            if (detailVideo.getSize() > ONE_GB) {
                throw BusinessException.badRequest("详述录像不能超过1GB");
            }
        }
    }

    private void validateExperienceArchives(
        MultipartFile reviewOriginal,
        MultipartFile proofArchive,
        List<MultipartFile> legacyFiles,
        Certification certification
    ) {
        boolean hasExistingReviewOriginal = certification.getMaterials().stream()
            .anyMatch(material -> material.getDeletedAt() == null
                && "REVIEW_ORIGINAL_ARCHIVE".equals(material.getMaterialKind()));
        boolean hasExistingProofArchive = certification.getMaterials().stream()
            .anyMatch(material -> material.getDeletedAt() == null
                && List.of("ARCHIVE", "PROOF_ARCHIVE").contains(material.getMaterialKind()));
        MultipartFile normalizedProofArchive = proofArchive;
        if ((normalizedProofArchive == null || normalizedProofArchive.isEmpty())
            && legacyFiles.size() == 1) {
            normalizedProofArchive = legacyFiles.get(0);
        }
        if (legacyFiles.size() > 1) {
            throw BusinessException.badRequest("已处理证明资料只能上传一个压缩包");
        }
        validateArchive(
            reviewOriginal,
            hasExistingReviewOriginal,
            "请上传未处理证明资料压缩包"
        );
        validateArchive(
            normalizedProofArchive,
            hasExistingProofArchive,
            "请上传已处理证明资料压缩包"
        );
    }

    private void validateArchive(
        MultipartFile archive,
        boolean hasExisting,
        String missingMessage
    ) {
        if ((archive == null || archive.isEmpty()) && !hasExisting) {
            throw BusinessException.badRequest(missingMessage);
        }
        if (archive == null || archive.isEmpty()) return;
        fileTypeDetector.requireArchive(archive);
        if (archive.getSize() > TWO_GB) {
            throw BusinessException.badRequest("每个压缩包不能超过2GB");
        }
    }

    private Certification requireUpgradeSource(Long userId, Long existingId, Long upgradeSourceId) {
        if (upgradeSourceId == null) return null;
        if (existingId != null) {
            Certification existing = certificationRepository.findByIdAndUserId(existingId, userId)
                .orElseThrow(() -> BusinessException.notFound("经历不存在"));
            if (!upgradeSourceId.equals(existing.getUpgradeSourceId())) {
                throw BusinessException.badRequest("升级来源不能变更");
            }
        } else if (certificationRepository.existsByUpgradeSourceIdAndDeletedAtIsNull(upgradeSourceId)) {
            throw BusinessException.badRequest("该公益分享已经申请升级");
        }
        return certificationRepository.findByIdAndUserId(upgradeSourceId, userId)
            .filter(item -> "EXPERIENCE".equals(item.getCategory()))
            .filter(item -> "PUBLIC_WELFARE".equals(normalizedBusinessType(item)))
            .filter(item -> "APPROVED".equals(item.getStatus()))
            .filter(Certification::isEnabled)
            .orElseThrow(() -> BusinessException.badRequest("只有已通过的公益分享才能升级"));
    }

    private boolean hasMaterial(Certification certification, String kind) {
        return certification.getMaterials().stream()
            .anyMatch(material -> material.getDeletedAt() == null
                && kind.equals(material.getMaterialKind()));
    }

    private void copyMaterialIfMissing(
        Certification source,
        Certification target,
        String kind
    ) {
        if (hasMaterial(target, kind)) return;
        source.getMaterials().stream()
            .filter(material -> material.getDeletedAt() == null)
            .filter(material -> kind.equals(material.getMaterialKind()))
            .findFirst()
            .ifPresent(material -> {
                CertificationMaterial copy = new CertificationMaterial();
                copy.setCertification(target);
                copy.setMaterialKind(material.getMaterialKind());
                copy.setOriginalName(material.getOriginalName());
                copy.setStorageKey(material.getStorageKey());
                copy.setPublicUrl(material.getPublicUrl());
                copy.setContentType(material.getContentType());
                copy.setFileSize(material.getFileSize());
                target.getMaterials().add(copy);
            });
    }

    private String normalizedBusinessType(Certification certification) {
        String type = certification.getExperienceBusinessType();
        return type == null || type.isBlank() ? "MONETIZED" : type;
    }

    private void validateSignature(MultipartFile signature) {
        if (signature == null || signature.isEmpty()) {
            throw BusinessException.badRequest("请完成本人手写签字");
        }
        fileTypeDetector.requireImage(signature);
        if (signature.getSize() > 5L * 1024 * 1024) {
            throw BusinessException.badRequest("签字图片不能超过5MB");
        }
    }

    private void refreshAnswererStatus(User user, String reviewedCertificationType) {
        List<Certification> certifications = certificationRepository
                .findByUserIdAndStatusAndEnabledTrueOrderByIdAsc(user.getId(), "APPROVED");
        boolean identity = certifications.stream().anyMatch(item -> "IDENTITY".equals(item.getCertificationType()));
        boolean experience = certifications.stream().anyMatch(item ->
            "EXPERIENCE".equals(item.getCategory())
                && "MONETIZED".equals(normalizedBusinessType(item))
        );
        boolean answererQualified = identity && experience;
        boolean reviewedQualification = List.of("IDENTITY", "EXPERIENCE").contains(reviewedCertificationType);

        user.setAnswererStatus(answererQualified ? "APPROVED" : "PENDING");
        if (!answererQualified || !"ACTIVE".equals(user.getAccountStatus())) {
            user.setAcceptingInquiries(false);
        } else if (reviewedQualification && user.getInquiryPriceUpdatedAt() != null) {
            user.setAcceptingInquiries(true);
        }
        userRepository.save(user);
    }

    private boolean isImage(MultipartFile file) {
        return "IMAGE".equals(fileTypeDetector.detect(file).kind());
    }

    private boolean isVideo(MultipartFile file) {
        return "VIDEO".equals(fileTypeDetector.detect(file).kind());
    }

    private String kindOf(MultipartFile file) {
        return fileTypeDetector.detect(file).kind();
    }

    public record MaterialView(Long id, String kind, String name, String url, long size, String contentType) {
        private static final List<String> EXPERIENCE_PROOF_KINDS = List.of(
            "ARCHIVE",
            "PROOF_ARCHIVE",
            "REVIEW_ORIGINAL_ARCHIVE"
        );

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
                    material.getId(), material.getMaterialKind(), material.getOriginalName(),
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
            boolean required,
            String status,
            boolean enabled,
            String rejectionReason,
            String experienceBusinessType,
            Long upgradeSourceId,
            String mediaProcessingStatus,
            String mediaProcessingError,
            LocalDateTime lastOperatedAt,
            List<MaterialView> materials) {
        static CertificationView from(Certification certification, FileStorage fileStorage) {
            return new CertificationView(
                    certification.getId(), certification.getCategory(), certification.getCertificationType(),
                    certification.getTitle(), certification.getDescription(),
                    certification.isRequiredItem(), certification.getStatus(), certification.isEnabled(),
                    certification.getRejectionReason(),
                    certification.getCategory().equals("EXPERIENCE")
                        ? certification.getExperienceBusinessType() == null
                            ? "MONETIZED"
                            : certification.getExperienceBusinessType()
                        : null,
                    certification.getUpgradeSourceId(),
                    certification.getMediaProcessingStatus(),
                    certification.getMediaProcessingError(),
                    resolveLastOperatedAt(certification),
                    certification.getMaterials().stream().filter(material -> material.getDeletedAt() == null)
                            .map(material -> MaterialView.from(
                                material,
                                fileStorage,
                                "APPROVED".equals(certification.getStatus())
                            )).toList());
        }
    }
}
