package com.shixianwen.user;

import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.common.BusinessException;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.certification.CertificationPublicMediaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Locale;

@Service
public class AnswererService {
    private static final String QUALIFIED =
        "EXISTS (SELECT 1 FROM certifications ci WHERE ci.user_id=u.id AND ci.certification_type='IDENTITY' AND ci.status='APPROVED' AND ci.enabled=TRUE AND ci.deleted_at IS NULL) " +
        "AND EXISTS (SELECT 1 FROM certifications cq WHERE cq.user_id=u.id AND cq.category='EXPERIENCE' " +
        "AND COALESCE(cq.experience_business_type,'MONETIZED')='MONETIZED' " +
        "AND cq.status='APPROVED' AND cq.enabled=TRUE AND cq.deleted_at IS NULL) ";
    private final UserRepository userRepository;
    private final CertificationRepository certificationRepository;
    private final JdbcTemplate jdbc;
    private final FileStorage fileStorage;
    private final CertificationPublicMediaService publicMediaService;

    public AnswererService(
        UserRepository userRepository,
        CertificationRepository certificationRepository,
        JdbcTemplate jdbc,
        FileStorage fileStorage,
        CertificationPublicMediaService publicMediaService
    ) {
        this.userRepository = userRepository;
        this.certificationRepository = certificationRepository;
        this.jdbc = jdbc;
        this.fileStorage = fileStorage;
        this.publicMediaService = publicMediaService;
    }

    @Transactional(readOnly = true)
    public AnswererPage search(
        Long currentUserId,
        String keyword,
        String experienceType,
        int page,
        int size
    ) {
        String businessTypeFilter = businessTypeFilter(experienceType);
        String accountType = accountType(currentUserId);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        List<ExperienceCardRow> rows = jdbc.query(
            "SELECT ce.id AS certification_id,u.id AS user_id FROM certifications ce " +
                "JOIN users u ON u.id=ce.user_id " +
                "WHERE ce.category='EXPERIENCE' AND ce.status='APPROVED' AND ce.enabled=TRUE AND ce.deleted_at IS NULL " +
                "AND u.id<>? AND u.account_type=? AND u.account_status='ACTIVE' " +
                "AND (COALESCE(ce.experience_business_type,'MONETIZED')='PUBLIC_WELFARE' " +
                "OR (COALESCE(ce.experience_business_type,'MONETIZED')='MONETIZED' AND u.accepting_inquiries=TRUE AND " + QUALIFIED + ")) " +
                "AND (?='' OR COALESCE(ce.experience_business_type,'MONETIZED')=?) " +
                "AND (?='' OR ce.title LIKE CONCAT('%',?,'%')) " +
                "ORDER BY ce.id DESC LIMIT ? OFFSET ?",
            (rs, rowNum) -> new ExperienceCardRow(rs.getLong("certification_id"), rs.getLong("user_id")),
            currentUserId, accountType,
            businessTypeFilter, businessTypeFilter,
            normalizedKeyword, normalizedKeyword,
            safeSize + 1, safePage * safeSize
        );
        boolean hasMore = rows.size() > safeSize;
        List<AnswererView> items = rows.stream().limit(safeSize)
            .map(row -> userRepository.findById(row.userId())
                .map(user -> toView(user, row.certificationId())))
            .flatMap(java.util.Optional::stream)
            .toList();
        return new AnswererPage(items, safePage, hasMore);
    }

    @Transactional(readOnly = true)
    public AnswererView detail(Long currentUserId, String uid) {
        String accountType = accountType(currentUserId);
        User user = userRepository.findByUidAndAccountStatus(uid, "ACTIVE")
            .filter(item -> accountType.equals(item.getAccountType()))
            .filter(item -> hasPublishedExperience(item.getId()))
            .orElseThrow(() -> BusinessException.notFound("该用户或经历不存在"));
        return toView(user);
    }

    private AnswererView toView(User user) {
        return toView(user, null);
    }

    private AnswererView toView(User user, Long onlyExperienceCertificationId) {
        List<Certification> certifications =
            certificationRepository.findByUserIdAndStatusAndEnabledTrueOrderByIdAsc(user.getId(), "APPROVED");
        List<ExperienceView> experiences = certifications.stream()
            .filter(item -> "EXPERIENCE".equals(item.getCategory()))
            .filter(item -> onlyExperienceCertificationId == null || onlyExperienceCertificationId.equals(item.getId()))
            .map(item -> new ExperienceView(
                item.getId(), item.getTitle(), item.getDescription(),
                normalizedBusinessType(item),
                "MONETIZED".equals(normalizedBusinessType(item))
                    && user.isAcceptingInquiries()
                    && currentQualification(user.getId()),
                item.getMaterials().stream()
                    .filter(material -> material.getDeletedAt() == null)
                    .filter(material -> List.of("DETAIL_VIDEO")
                        .contains(material.getMaterialKind()))
                    .map(material -> new ExperienceMaterialView(
                        material.getId(),
                        material.getMaterialKind(),
                        material.getOriginalName(),
                        material.getPublicUrl() == null || material.getPublicUrl().isBlank()
                            ? fileStorage.accessUrl(material.getStorageKey(), StorageVisibility.PRIVATE)
                            : material.getPublicUrl(),
                        material.getFileSize(),
                        material.getContentType()
                    ))
                    .toList(),
                publicMediaService.publicItems(item.getId()).stream()
                    .map(media -> new ExperienceMaterialView(
                        media.id(), media.kind(), media.name(), media.url(),
                        media.size(), media.contentType()
                    ))
                    .toList()
            ))
            .toList();
        return new AnswererView(
            user.getId(), user.getUid(), user.getNickname(), user.getAvatarUrl(),
            certifications.stream().anyMatch(item -> "IDENTITY".equals(item.getCertificationType())),
            user.isAcceptingInquiries(),
            user.getInquiryPriceMin(), user.getInquiryPriceMax(),
            user.getJobTitle(), experiences
        );
    }

    private boolean currentQualification(Long userId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users u WHERE u.id=? AND " + QUALIFIED,
            Integer.class,
            userId
        );
        return count != null && count > 0;
    }

    private boolean hasPublishedExperience(Long userId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM certifications WHERE user_id=? AND category='EXPERIENCE' " +
                "AND status='APPROVED' AND enabled=TRUE AND deleted_at IS NULL",
            Integer.class,
            userId
        );
        return count != null && count > 0;
    }

    private String normalizedBusinessType(Certification item) {
        String value = item.getExperienceBusinessType();
        return value == null || value.isBlank() ? "MONETIZED" : value;
    }

    static String businessTypeFilter(String value) {
        String normalized = value == null
            ? "ALL"
            : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ALL" -> "";
            case "FREE" -> "PUBLIC_WELFARE";
            case "PAID" -> "MONETIZED";
            default -> throw BusinessException.badRequest("经历筛选条件不正确");
        };
    }

    private String accountType(Long userId) {
        return userRepository.findById(userId)
            .map(User::getAccountType)
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }

    private String fileExtension(String fileName) {
        if (fileName == null) return ".zip";
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) return ".zip";
        String extension = fileName.substring(index).toLowerCase();
        return List.of(".zip", ".rar").contains(extension) ? extension : ".zip";
    }

    private record ExperienceCardRow(Long certificationId, Long userId) {}

    public record ExperienceView(
        Long certificationId,
        String title,
        String description,
        String businessType,
        boolean canInquire,
        List<ExperienceMaterialView> materials,
        List<ExperienceMaterialView> publicMedia
    ) {
    }

    public record ExperienceMaterialView(
        Long id,
        String kind,
        String name,
        String url,
        long size,
        String contentType
    ) {
    }

    public record AnswererView(
        Long id,
        String uid,
        String nickname,
        String avatarUrl,
        boolean identityVerified,
        boolean acceptingInquiries,
        int inquiryPriceMin,
        int inquiryPriceMax,
        String mainJob,
        List<ExperienceView> experiences
    ) {
    }

    public record AnswererPage(List<AnswererView> items, int page, boolean hasMore) {}
}
