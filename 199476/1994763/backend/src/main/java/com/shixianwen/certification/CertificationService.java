package com.shixianwen.certification;

import com.shixianwen.common.BusinessException;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StoredFile;
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

    public CertificationService(
        CertificationRepository certificationRepository,
        UserRepository userRepository,
        FileStorage fileStorage
    ) {
        this.certificationRepository = certificationRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
    }

    @Transactional(readOnly = true)
    public List<CertificationView> list(User user) {
        return certificationRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
            .map(CertificationView::from)
            .toList();
    }

    @Transactional
    public CertificationView submitBasic(
        User user,
        String type,
        String title,
        Integer years,
        List<MultipartFile> files
    ) {
        String normalizedType = type.toUpperCase(Locale.ROOT);
        if (!List.of("IDENTITY", "MAIN_JOB").contains(normalizedType)) {
            throw BusinessException.badRequest("基础认证类型不正确");
        }
        if (files.size() != 3 || files.stream().anyMatch(file -> !isImage(file))) {
            throw BusinessException.badRequest("基础信息认证需要按要求提交3张图片");
        }
        if ("MAIN_JOB".equals(normalizedType) && (title == null || title.isBlank())) {
            throw BusinessException.badRequest("岗位名称不能为空");
        }

        Certification certification = baseCertification(
            user,
            "BASIC",
            normalizedType,
            "IDENTITY".equals(normalizedType) ? "实名认证" : title.trim(),
            true
        );
        certification.setYears(years);
        attachFiles(certification, files);
        return CertificationView.from(certificationRepository.save(certification));
    }

    @Transactional
    public CertificationView submitExperience(
        User user,
        String title,
        String description,
        Integer years,
        List<MultipartFile> files
    ) {
        if (title == null || title.isBlank()) throw BusinessException.badRequest("请填写具体经历");
        validateExperienceFiles(files);
        Certification certification = baseCertification(
            user,
            "EXPERIENCE",
            "EXPERIENCE",
            title.trim(),
            false
        );
        certification.setDescription(description == null ? null : description.trim());
        certification.setYears(years);
        attachFiles(certification, files);
        return CertificationView.from(certificationRepository.save(certification));
    }

    @Transactional
    public CertificationView review(Long id, boolean approved, String reason) {
        Certification certification = certificationRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("认证不存在"));
        certification.setStatus(approved ? "APPROVED" : "REJECTED");
        certification.setRejectionReason(approved ? null : reason);
        certification.setReviewedAt(LocalDateTime.now());
        certification = certificationRepository.save(certification);
        refreshAnswererStatus(certification.getUser());
        return CertificationView.from(certification);
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
            StoredFile stored = fileStorage.store(file, "certifications/" + certification.getUser().getUid());
            CertificationMaterial material = new CertificationMaterial();
            material.setCertification(certification);
            material.setMaterialKind(kindOf(file));
            material.setOriginalName(file.getOriginalFilename() == null ? "材料" : file.getOriginalFilename());
            material.setStorageKey(stored.storageKey());
            material.setContentType(stored.contentType());
            material.setFileSize(stored.size());
            certification.getMaterials().add(material);
        }
    }

    private void validateExperienceFiles(List<MultipartFile> files) {
        long archives = files.stream().filter(this::isArchive).count();
        long videos = files.stream().filter(this::isVideo).count();
        long images = files.stream().filter(this::isImage).count();
        if (archives > 1 || videos > 1 || images > 5 || archives + videos + images != files.size()) {
            throw BusinessException.badRequest("经历材料最多支持1个压缩包、1段录像和5张照片");
        }
        files.stream().filter(this::isArchive).filter(file -> file.getSize() > ONE_GB).findAny()
            .ifPresent(file -> { throw BusinessException.badRequest("压缩包不能超过1GB"); });
        files.stream().filter(this::isVideo).filter(file -> file.getSize() > FIVE_HUNDRED_MB).findAny()
            .ifPresent(file -> { throw BusinessException.badRequest("录像不能超过500MB"); });
    }

    private void refreshAnswererStatus(User user) {
        List<Certification> certifications = certificationRepository.findByUserIdAndStatusOrderByIdAsc(user.getId(), "APPROVED");
        boolean identity = certifications.stream().anyMatch(item -> "IDENTITY".equals(item.getCertificationType()));
        boolean mainJob = certifications.stream().anyMatch(item -> "MAIN_JOB".equals(item.getCertificationType()));
        user.setAnswererStatus(identity && mainJob ? "APPROVED" : "PENDING");
        userRepository.save(user);
    }

    private boolean isImage(MultipartFile file) {
        return file.getContentType() != null && file.getContentType().startsWith("image/");
    }

    private boolean isVideo(MultipartFile file) {
        return file.getContentType() != null && file.getContentType().startsWith("video/");
    }

    private boolean isArchive(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        return name.endsWith(".zip") || name.endsWith(".rar");
    }

    private String kindOf(MultipartFile file) {
        if (isImage(file)) return "IMAGE";
        if (isVideo(file)) return "VIDEO";
        return "ARCHIVE";
    }

    public record MaterialView(Long id, String kind, String name, String url, long size, String contentType) {
        static MaterialView from(CertificationMaterial material) {
            return new MaterialView(
                material.getId(), material.getMaterialKind(), material.getOriginalName(),
                "/uploads/" + material.getStorageKey(), material.getFileSize(), material.getContentType()
            );
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
        String rejectionReason,
        List<MaterialView> materials
    ) {
        static CertificationView from(Certification certification) {
            return new CertificationView(
                certification.getId(), certification.getCategory(), certification.getCertificationType(),
                certification.getTitle(), certification.getDescription(), certification.getYears(),
                certification.isRequiredItem(), certification.getStatus(), certification.getRejectionReason(),
                certification.getMaterials().stream().map(MaterialView::from).toList()
            );
        }
    }
}
