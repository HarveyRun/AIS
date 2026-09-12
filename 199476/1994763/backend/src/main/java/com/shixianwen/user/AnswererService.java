package com.shixianwen.user;

import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.common.BusinessException;
import com.shixianwen.certification.CertificationPublicMediaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Service
public class AnswererService {
    private static final String QUALIFIED =
        "EXISTS (SELECT 1 FROM certifications cq WHERE cq.user_id=u.id AND cq.category='EXPERIENCE' " +
        "AND cq.status='APPROVED' AND cq.enabled=TRUE AND cq.deleted_at IS NULL) ";
    private final UserRepository userRepository;
    private final CertificationRepository certificationRepository;
    private final JdbcTemplate jdbc;
    private final CertificationPublicMediaService publicMediaService;

    public AnswererService(
        UserRepository userRepository,
        CertificationRepository certificationRepository,
        JdbcTemplate jdbc,
        CertificationPublicMediaService publicMediaService
    ) {
        this.userRepository = userRepository;
        this.certificationRepository = certificationRepository;
        this.jdbc = jdbc;
        this.publicMediaService = publicMediaService;
    }

    @Transactional(readOnly = true)
    public AnswererPage search(
        Long currentUserId,
        String keyword,
        int page,
        int size
    ) {
        String accountType = accountType(currentUserId);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        List<ExperienceCardRow> rows = jdbc.query(
            "SELECT ce.id AS certification_id,u.id AS user_id FROM certifications ce " +
                "JOIN users u ON u.id=ce.user_id " +
                "WHERE ce.category='EXPERIENCE' AND ce.status='APPROVED' AND ce.enabled=TRUE AND ce.deleted_at IS NULL " +
                "AND u.id<>? AND u.account_type=? AND u.account_status='ACTIVE' AND u.accepting_inquiries=TRUE " +
                "AND (?='' OR ce.title LIKE CONCAT('%',?,'%')) " +
                "ORDER BY ce.id DESC LIMIT ? OFFSET ?",
            (rs, rowNum) -> new ExperienceCardRow(rs.getLong("certification_id"), rs.getLong("user_id")),
            currentUserId, accountType,
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
            .filter(User::isAcceptingInquiries)
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
                item.getExperienceLocation(), item.getExperienceStartDate(), item.getExperienceEndDate(),
                item.getExperienceCount(), item.getExperienceRole(), item.getExperienceAgeRange(),
                item.getExperienceEducation(), item.getExperienceJob(),
                user.isAcceptingInquiries()
                    && currentQualification(user.getId()),
                List.of(),
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
            user.isAcceptingInquiries(),
            user.getInquiryHourlyRate(),
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

    private String accountType(Long userId) {
        return userRepository.findById(userId)
            .map(User::getAccountType)
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }

    private record ExperienceCardRow(Long certificationId, Long userId) {}

    public record ExperienceView(
        Long certificationId,
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
        boolean acceptingInquiries,
        int inquiryHourlyRate,
        String mainJob,
        List<ExperienceView> experiences
    ) {
    }

    public record AnswererPage(List<AnswererView> items, int page, boolean hasMore) {}
}
