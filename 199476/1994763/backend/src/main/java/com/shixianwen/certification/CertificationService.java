package com.shixianwen.certification;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CertificationService {
    private static final long ONE_GB = 1024L * 1024 * 1024;
    private static final long FIVE_HUNDRED_MB = 500L * 1024 * 1024;

    private final CertificationRepository certificationRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;
    private final SensitiveWordService sensitiveWords;
    private final FileTypeDetector fileTypeDetector;

    public CertificationService(
            CertificationRepository certificationRepository,
            UserRepository userRepository,
            FileStorage fileStorage,
            SensitiveWordService sensitiveWords,
            FileTypeDetector fileTypeDetector) {
        this.certificationRepository = certificationRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
        this.sensitiveWords = sensitiveWords;
        this.fileTypeDetector = fileTypeDetector;
    }

    @Transactional(readOnly = true)
    public List<CertificationView> list(User user) {
        return certificationRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
                .map(this::view)
                .toList();
    }

    @Transactional
    public CertificationView submitBasic(
            User user,
            String type,
            String title,
            Integer years,
            List<MultipartFile> files) {
        String normalizedType = type.toUpperCase(Locale.ROOT);
        if (!List.of("IDENTITY", "MAIN_JOB").contains(normalizedType)) {
            throw BusinessException.badRequest("基础认证类型不正确");
        }
        if ("IDENTITY".equals(normalizedType) &&
                (files.size() != 3 || files.stream().anyMatch(file -> !isImage(file)))) {
            throw BusinessException.badRequest("身份信息认证需要按要求提交3张图片");
        }
        if ("MAIN_JOB".equals(normalizedType)) {
            validateJobFiles(files);
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
                        "IDENTITY".equals(normalizedType) ? "实名认证" : "",
                        true)
                : existing;
        certification.setTitle("IDENTITY".equals(normalizedType) ? "实名认证" : "");
        certification.setStatus("PENDING");
        certification.setRejectionReason(null);
        certification.setSubmittedAt(LocalDateTime.now());
        retireMaterials(certification);
        certification.setYears(years);
        attachFiles(certification, files);
        return view(certificationRepository.save(certification));
    }

    @Transactional
    public CertificationView submitExperience(
            User user,
            Long existingId,
            String title,
            String description,
            Integer years,
            List<MultipartFile> files) {
        boolean identityVerified = certificationRepository
                .existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
                        user.getId(),
                        "IDENTITY",
                        "APPROVED");
        if (!identityVerified) {
            throw BusinessException.badRequest("完成实名认证后才能添加亲身经历");
        }
        if (title == null || title.isBlank())
            throw BusinessException.badRequest("请填写经历标题");
        if (title.trim().length() > 50)
            throw BusinessException.badRequest("经历标题最多50个字");
        if (description == null || description.isBlank())
            throw BusinessException.badRequest("请填写经历简述");
        if (description.trim().length() > 300)
            throw BusinessException.badRequest("经历简述最多300个字");
        validateExperienceFiles(files);
        Certification certification;
        if (existingId == null) {
            certification = baseCertification(user, "EXPERIENCE", "EXPERIENCE", sensitiveWords.mask(title.trim()), false);
        } else {
            certification = certificationRepository.findByIdAndUserId(existingId, user.getId())
                    .filter(item -> "EXPERIENCE".equals(item.getCategory()) && "REJECTED".equals(item.getStatus()))
                    .orElseThrow(() -> BusinessException.badRequest("该经历不能重新提交"));
            retireMaterials(certification);
            certification.setTitle(sensitiveWords.mask(title.trim()));
            certification.setStatus("PENDING");
            certification.setRejectionReason(null);
            certification.setSubmittedAt(LocalDateTime.now());
        }
        certification.setDescription(description == null ? null : sensitiveWords.mask(description.trim()));
        certification.setYears(years);
        attachFiles(certification, files);
        return view(certificationRepository.save(certification));
    }

    @Transactional
    public CertificationView review(Long id, boolean approved, String reason) {
        Certification certification = certificationRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("认证不存在"));
        certification.setStatus(approved ? "APPROVED" : "REJECTED");
        certification.setRejectionReason(approved ? null : reason);
        certification.setReviewedAt(LocalDateTime.now());
        certification = certificationRepository.save(certification);
        refreshAnswererStatus(certification.getUser(), certification.getCertificationType());
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
            StoredFile stored = fileStorage.store(
                file,
                storagePrefix(certification.getUser()) + "certifications/" + certification.getUser().getUid(),
                StorageVisibility.PRIVATE
            );
            CertificationMaterial material = new CertificationMaterial();
            material.setCertification(certification);
            material.setMaterialKind(kindOf(file));
            material.setOriginalName(file.getOriginalFilename() == null ? "材料" : file.getOriginalFilename());
            material.setStorageKey(stored.storageKey());
            material.setPublicUrl(null);
            material.setContentType(stored.contentType());
            material.setFileSize(stored.size());
            certification.getMaterials().add(material);
        }
    }

    private String storagePrefix(User user) {
        return "TEST".equals(user.getAccountType()) ? "test/" : "";
    }

    private CertificationView view(Certification certification) {
        return CertificationView.from(certification, fileStorage);
    }

    private void retireMaterials(Certification certification) {
        LocalDateTime now = LocalDateTime.now();
        certification.getMaterials().forEach(material -> material.setDeletedAt(now));
    }

    private void validateExperienceFiles(List<MultipartFile> files) {
        long archives = files.stream().filter(this::isArchive).count();
        long videos = files.stream().filter(this::isVideo).count();
        long images = files.stream().filter(this::isImage).count();
        if (archives != 1 || videos > 1 || images > 5 || archives + videos + images != files.size()) {
            throw BusinessException.badRequest("经历认证必须上传1个压缩包，另可提交1段录像和最多5张照片");
        }
        files.stream().filter(this::isArchive).filter(file -> file.getSize() > ONE_GB).findAny()
                .ifPresent(file -> {
                    throw BusinessException.badRequest("压缩包不能超过1GB");
                });
        files.stream().filter(this::isVideo).filter(file -> file.getSize() > FIVE_HUNDRED_MB).findAny()
                .ifPresent(file -> {
                    throw BusinessException.badRequest("录像不能超过500MB");
                });
    }

    private void validateJobFiles(List<MultipartFile> files) {
        long videos = files.stream().filter(this::isVideo).count();
        long images = files.stream().filter(this::isImage).count();
        if (videos + images == 0)
            throw BusinessException.badRequest("岗位认证至少需要一段录像或一张照片");
        if (videos > 1 || images > 5 || videos + images != files.size()) {
            throw BusinessException.badRequest("岗位材料只支持1段录像和最多5张照片");
        }
        files.stream().filter(this::isVideo).filter(file -> file.getSize() > FIVE_HUNDRED_MB).findAny()
                .ifPresent(file -> {
                    throw BusinessException.badRequest("录像不能超过500MB");
                });
    }

    private void refreshAnswererStatus(User user, String reviewedCertificationType) {
        List<Certification> certifications = certificationRepository
                .findByUserIdAndStatusAndEnabledTrueOrderByIdAsc(user.getId(), "APPROVED");
        boolean identity = certifications.stream().anyMatch(item -> "IDENTITY".equals(item.getCertificationType()));
        boolean mainJob = certifications.stream().anyMatch(item -> "MAIN_JOB".equals(item.getCertificationType()));
        boolean basicInformationApproved = identity && mainJob;
        boolean reviewedBasicInformation = List.of("IDENTITY", "MAIN_JOB").contains(reviewedCertificationType);

        user.setAnswererStatus(basicInformationApproved ? "APPROVED" : "PENDING");
        if (!basicInformationApproved || !"ACTIVE".equals(user.getAccountStatus())) {
            user.setAcceptingInquiries(false);
        } else if (reviewedBasicInformation && user.getInquiryPriceUpdatedAt() != null) {
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

    private boolean isArchive(MultipartFile file) {
        return "ARCHIVE".equals(fileTypeDetector.detect(file).kind());
    }

    private String kindOf(MultipartFile file) {
        return fileTypeDetector.detect(file).kind();
    }

    public record MaterialView(Long id, String kind, String name, String url, long size, String contentType) {
        static MaterialView from(CertificationMaterial material, FileStorage fileStorage) {
            String legacyUrl = material.getPublicUrl();
            String url = legacyUrl == null || legacyUrl.isBlank()
                ? fileStorage.accessUrl(material.getStorageKey(), StorageVisibility.PRIVATE)
                : legacyUrl;
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
            Integer years,
            boolean required,
            String status,
            boolean enabled,
            String rejectionReason,
            List<MaterialView> materials) {
        static CertificationView from(Certification certification, FileStorage fileStorage) {
            return new CertificationView(
                    certification.getId(), certification.getCategory(), certification.getCertificationType(),
                    certification.getTitle(), certification.getDescription(), certification.getYears(),
                    certification.isRequiredItem(), certification.getStatus(), certification.isEnabled(),
                    certification.getRejectionReason(),
                    certification.getMaterials().stream().filter(material -> material.getDeletedAt() == null)
                            .map(material -> MaterialView.from(material, fileStorage)).toList());
        }
    }
}
