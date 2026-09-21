package com.shixianwen.user;

import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.common.BusinessException;
import com.shixianwen.certification.CertificationPublicMediaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Locale;

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
        return search(currentUserId, keyword, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public AnswererPage search(
        Long currentUserId,
        String keyword,
        String sortBy,
        String sortDirection,
        int page,
        int size
    ) {
        String accountType = accountType(currentUserId);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedSortBy = normalizeSortBy(sortBy);
        String normalizedDirection = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        String orderBy = switch (normalizedSortBy) {
            case "REFERENCE_INDEX" ->
                "ce.reference_index IS NULL ASC,ce.reference_index " + normalizedDirection + ",ce.id DESC";
            case "LIKE_COUNT" -> "COALESCE(el.like_count,0) " + normalizedDirection + ",ce.id DESC";
            default -> "ce.id DESC";
        };
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        List<ExperienceCardRow> rows = jdbc.query(
            "SELECT ce.id AS certification_id,u.id AS user_id FROM certifications ce " +
                "JOIN users u ON u.id=ce.user_id " +
                "LEFT JOIN (SELECT certification_id,COUNT(*) AS like_count FROM experience_likes " +
                "WHERE active=TRUE GROUP BY certification_id) el ON el.certification_id=ce.id " +
                "WHERE ce.category='EXPERIENCE' AND ce.status='APPROVED' AND ce.enabled=TRUE AND ce.deleted_at IS NULL " +
                "AND u.id<>? AND u.account_type=? AND u.account_status='ACTIVE' AND u.accepting_inquiries=TRUE " +
                "AND (?='' OR ce.title LIKE CONCAT('%',?,'%')) " +
                "ORDER BY " + orderBy + " LIMIT ? OFFSET ?",
            (rs, rowNum) -> new ExperienceCardRow(rs.getLong("certification_id"), rs.getLong("user_id")),
            currentUserId, accountType,
            normalizedKeyword, normalizedKeyword,
            safeSize + 1, safePage * safeSize
        );
        boolean hasMore = rows.size() > safeSize;
        List<AnswererView> items = rows.stream().limit(safeSize)
            .map(row -> userRepository.findById(row.userId())
                .map(user -> toView(currentUserId, user, row.certificationId())))
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
        return toView(currentUserId, user);
    }

    @Transactional
    public ExperienceLikeView setExperienceLike(
        Long currentUserId,
        String ownerUid,
        Long certificationId,
        boolean liked
    ) {
        String accountType = accountType(currentUserId);
        LikeTarget target = jdbc.query(
            "SELECT c.user_id,u.uid,u.account_type,u.account_status,u.accepting_inquiries " +
                "FROM certifications c JOIN users u ON u.id=c.user_id " +
                "WHERE c.id=? AND c.category='EXPERIENCE' AND c.status='APPROVED' " +
                "AND c.enabled=TRUE AND c.deleted_at IS NULL",
            (rs, rowNum) -> new LikeTarget(
                rs.getLong("user_id"), rs.getString("uid"), rs.getString("account_type"),
                rs.getString("account_status"), rs.getBoolean("accepting_inquiries")
            ),
            certificationId
        ).stream().findFirst().orElseThrow(() -> BusinessException.notFound("经历不存在"));
        if (!target.uid().equals(ownerUid)
            || !accountType.equals(target.accountType())
            || !"ACTIVE".equals(target.accountStatus())
            || !target.acceptingInquiries()) {
            throw BusinessException.notFound("经历不存在");
        }
        if (target.userId().equals(currentUserId)) {
            throw BusinessException.badRequest("不能给自己的经历点赞");
        }
        jdbc.update(
            "INSERT INTO experience_likes(certification_id,user_id,active,created_at,updated_at) " +
                "VALUES (?,?,?,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6)) " +
                "ON DUPLICATE KEY UPDATE active=?,updated_at=CURRENT_TIMESTAMP(6)",
            certificationId, currentUserId, liked, liked
        );
        LikeStats stats = likeStats(currentUserId, certificationId);
        return new ExperienceLikeView(stats.likeCount(), stats.liked());
    }

    private AnswererView toView(Long currentUserId, User user) {
        return toView(currentUserId, user, null);
    }

    private AnswererView toView(Long currentUserId, User user, Long onlyExperienceCertificationId) {
        List<Certification> certifications =
            certificationRepository.findByUserIdAndStatusAndEnabledTrueOrderByIdAsc(user.getId(), "APPROVED");
        List<ExperienceView> experiences = certifications.stream()
            .filter(item -> "EXPERIENCE".equals(item.getCategory()))
            .filter(item -> onlyExperienceCertificationId == null || onlyExperienceCertificationId.equals(item.getId()))
            .map(item -> {
                LikeStats likeStats = likeStats(currentUserId, item.getId());
                return new ExperienceView(
                    item.getId(), item.getTitle(), item.getDescription(),
                    item.getReferenceIndex(), likeStats.likeCount(), likeStats.liked(),
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
                );
            })
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

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null) return "DEFAULT";
        String normalized = sortBy.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "REFERENCE_INDEX", "LIKE_COUNT" -> normalized;
            default -> "DEFAULT";
        };
    }

    private LikeStats likeStats(Long currentUserId, Long certificationId) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) AS like_count," +
                "COALESCE(MAX(CASE WHEN user_id=? THEN 1 ELSE 0 END),0) AS liked " +
                "FROM experience_likes WHERE certification_id=? AND active=TRUE",
            (rs, rowNum) -> new LikeStats(rs.getInt("like_count"), rs.getBoolean("liked")),
            currentUserId, certificationId
        );
    }

    private record ExperienceCardRow(Long certificationId, Long userId) {}
    private record LikeTarget(Long userId, String uid, String accountType, String accountStatus, boolean acceptingInquiries) {}
    private record LikeStats(int likeCount, boolean liked) {}

    public record ExperienceLikeView(int likeCount, boolean liked) {}

    public record ExperienceView(
        Long certificationId,
        String title,
        String description,
        Integer referenceIndex,
        int likeCount,
        boolean likedByCurrentUser,
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
