package com.shixianwen.user;

import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.certification.ExperienceCategoryService;
import com.shixianwen.common.BusinessException;
import com.shixianwen.certification.CertificationPublicMediaService;
import com.shixianwen.realtime.RealtimePublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDateTime;

@Service
public class AnswererService {
    private static final String QUALIFIED =
        "EXISTS (SELECT 1 FROM certifications cq WHERE cq.user_id=u.id AND cq.category='EXPERIENCE' " +
        "AND cq.status='APPROVED' AND cq.enabled=TRUE AND cq.deleted_at IS NULL) ";
    private final UserRepository userRepository;
    private final CertificationRepository certificationRepository;
    private final JdbcTemplate jdbc;
    private final CertificationPublicMediaService publicMediaService;
    private final ExperienceCategoryService experienceCategoryService;
    private final RealtimePublisher realtime;

    public AnswererService(
        UserRepository userRepository,
        CertificationRepository certificationRepository,
        JdbcTemplate jdbc,
        CertificationPublicMediaService publicMediaService,
        ExperienceCategoryService experienceCategoryService,
        RealtimePublisher realtime
    ) {
        this.userRepository = userRepository;
        this.certificationRepository = certificationRepository;
        this.jdbc = jdbc;
        this.publicMediaService = publicMediaService;
        this.experienceCategoryService = experienceCategoryService;
        this.realtime = realtime;
    }

    @Transactional(readOnly = true)
    public AnswererPage search(
        Long currentUserId,
        String keyword,
        int page,
        int size
    ) {
        return search(currentUserId, keyword, null, null, null, page, size);
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
        return search(currentUserId, keyword, sortBy, sortDirection, null, page, size);
    }

    @Transactional(readOnly = true)
    public AnswererPage search(
        Long currentUserId,
        String keyword,
        String sortBy,
        String sortDirection,
        Long categoryId,
        int page,
        int size
    ) {
        String accountType = accountType(currentUserId);
        List<Long> categoryLeafIds = categoryId == null ? List.of() : experienceCategoryService.matchingLeafIds(categoryId);
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
        List<Object> queryArgs = new ArrayList<>(List.of(currentUserId, accountType,
            normalizedKeyword, normalizedKeyword));
        String categoryCondition = "";
        if (categoryId != null) {
            categoryCondition = categoryLeafIds.isEmpty() ? "AND 1=0 " :
                "AND ce.experience_category_id IN (" +
                    String.join(",", Collections.nCopies(categoryLeafIds.size(), "?")) + ") ";
            queryArgs.addAll(categoryLeafIds);
        }
        queryArgs.add(safeSize + 1);
        queryArgs.add(safePage * safeSize);
        List<ExperienceCardRow> rows = jdbc.query(
            "SELECT ce.id AS certification_id,u.id AS user_id FROM certifications ce " +
                "JOIN users u ON u.id=ce.user_id " +
                "LEFT JOIN (SELECT certification_id,COUNT(*) AS like_count FROM experience_likes " +
                "WHERE active=TRUE GROUP BY certification_id) el ON el.certification_id=ce.id " +
                "WHERE ce.category='EXPERIENCE' AND ce.status='APPROVED' AND ce.enabled=TRUE AND ce.deleted_at IS NULL " +
                "AND u.id<>? AND u.account_type=? AND u.account_status='ACTIVE' AND u.accepting_inquiries=TRUE " +
                "AND (?='' OR ce.title LIKE CONCAT('%',?,'%')) " +
                categoryCondition +
                "ORDER BY " + orderBy + " LIMIT ? OFFSET ?",
            (rs, rowNum) -> new ExperienceCardRow(rs.getLong("certification_id"), rs.getLong("user_id")),
            queryArgs.toArray()
        );
        boolean hasMore = rows.size() > safeSize;
        List<AnswererView> items = rows.stream().limit(safeSize)
            .map(row -> userRepository.findById(row.userId())
                .map(user -> toView(currentUserId, user, row.certificationId())))
            .flatMap(java.util.Optional::stream)
            .toList();
        return new AnswererPage(items, safePage, hasMore);
    }

    @Transactional
    public AnswererView detail(Long currentUserId, String uid, Long experienceId) {
        String accountType = accountType(currentUserId);
        User user = userRepository.findByUidAndAccountStatus(uid, "ACTIVE")
            .filter(item -> accountType.equals(item.getAccountType()))
            .filter(User::isAcceptingInquiries)
            .filter(item -> hasPublishedExperience(item.getId()))
            .orElseThrow(() -> BusinessException.notFound("该用户或经历不存在"));
        Long selectedExperienceId = resolvePublishedExperience(user.getId(), experienceId);
        jdbc.update(
            "INSERT INTO experience_user_library(user_id,certification_id,viewed_at) " +
                "VALUES (?,?,CURRENT_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE viewed_at=CURRENT_TIMESTAMP(6)",
            currentUserId, selectedExperienceId
        );
        return toView(currentUserId, user, selectedExperienceId);
    }

    @Transactional(readOnly = true)
    public ExperienceLibraryPage library(Long currentUserId, String type, int page, int size) {
        String normalizedType = "RECENT".equalsIgnoreCase(type) ? "RECENT" : "FAVORITES";
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        String requiredColumn = "RECENT".equals(normalizedType) ? "viewed_at" : "favorited_at";
        List<ExperienceLibraryRow> rows = jdbc.query(
            "SELECT library.certification_id,c.user_id,library." + requiredColumn + " AS occurred_at " +
                "FROM experience_user_library library " +
                "JOIN certifications c ON c.id=library.certification_id " +
                "JOIN users u ON u.id=c.user_id " +
                "WHERE library.user_id=? AND library." + requiredColumn + " IS NOT NULL " +
                "AND c.category='EXPERIENCE' AND c.status='APPROVED' AND c.enabled=TRUE " +
                "AND c.deleted_at IS NULL AND u.account_status='ACTIVE' AND u.accepting_inquiries=TRUE " +
                "ORDER BY library." + requiredColumn + " DESC LIMIT ? OFFSET ?",
            (rs, rowNum) -> new ExperienceLibraryRow(
                rs.getLong("certification_id"), rs.getLong("user_id"),
                rs.getTimestamp("occurred_at").toLocalDateTime()
            ),
            currentUserId, safeSize + 1, safePage * safeSize
        );
        boolean hasMore = rows.size() > safeSize;
        List<ExperienceLibraryItem> items = rows.stream().limit(safeSize)
            .map(row -> userRepository.findById(row.userId())
                .map(user -> new ExperienceLibraryItem(
                    toView(currentUserId, user, row.certificationId()), row.occurredAt()
                )))
            .flatMap(java.util.Optional::stream)
            .toList();
        return new ExperienceLibraryPage(items, safePage, hasMore);
    }

    @Transactional
    public void deleteRecentView(Long currentUserId, Long certificationId) {
        jdbc.update(
            "UPDATE experience_user_library SET viewed_at=NULL WHERE user_id=? AND certification_id=?",
            currentUserId, certificationId
        );
        deleteEmptyLibraryRows(currentUserId);
    }

    @Transactional
    public void clearRecentViews(Long currentUserId) {
        jdbc.update(
            "UPDATE experience_user_library SET viewed_at=NULL WHERE user_id=? AND viewed_at IS NOT NULL",
            currentUserId
        );
        deleteEmptyLibraryRows(currentUserId);
    }

    private void deleteEmptyLibraryRows(Long currentUserId) {
        jdbc.update(
            "DELETE FROM experience_user_library WHERE user_id=? " +
                "AND viewed_at IS NULL AND favorited_at IS NULL",
            currentUserId
        );
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

    @Transactional
    public ExperienceFavoriteView setExperienceFavorite(
        Long currentUserId,
        String ownerUid,
        Long certificationId,
        boolean favorited
    ) {
        LikeTarget target = requireVisibleExperience(currentUserId, ownerUid, certificationId);
        if (target.userId().equals(currentUserId)) {
            throw BusinessException.badRequest("不能收藏自己的经历");
        }
        jdbc.update(
            "INSERT INTO experience_user_library(user_id,certification_id,favorited_at) " +
                "VALUES (?,?,CASE WHEN ? THEN CURRENT_TIMESTAMP(6) ELSE NULL END) " +
                "ON DUPLICATE KEY UPDATE favorited_at=CASE WHEN ? THEN CURRENT_TIMESTAMP(6) ELSE NULL END",
            currentUserId, certificationId, favorited, favorited
        );
        return new ExperienceFavoriteView(favorited);
    }

    private LikeTarget requireVisibleExperience(Long currentUserId, String ownerUid, Long certificationId) {
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
        return target;
    }

    private Long resolvePublishedExperience(Long ownerId, Long experienceId) {
        if (experienceId != null) {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM certifications WHERE id=? AND user_id=? " +
                    "AND category='EXPERIENCE' AND status='APPROVED' AND enabled=TRUE AND deleted_at IS NULL",
                Integer.class, experienceId, ownerId
            );
            if (count != null && count > 0) return experienceId;
            throw BusinessException.notFound("经历不存在");
        }
        return jdbc.query(
            "SELECT id FROM certifications WHERE user_id=? AND category='EXPERIENCE' " +
                "AND status='APPROVED' AND enabled=TRUE AND deleted_at IS NULL ORDER BY id DESC LIMIT 1",
            (rs, rowNum) -> rs.getLong("id"), ownerId
        ).stream().findFirst().orElseThrow(() -> BusinessException.notFound("经历不存在"));
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
                    isFavorited(currentUserId, item.getId()),
                    item.getExperienceLocation(), item.getExperienceStartDate(), item.getExperienceEndDate(),
                    item.getExperienceCount(), item.getExperienceRole(), item.getExperienceAgeRange(),
                    item.getExperienceEducation(), item.getExperienceJob(),
                    item.getExperienceCategory() == null ? null : item.getExperienceCategory().getId(),
                    item.getExperienceCategory() == null ? null : item.getExperienceCategory().getParent().getId(),
                    item.getExperienceCategory() == null ? null : item.getExperienceCategory().getName(),
                    item.getExperienceCategory() == null ? null : item.getExperienceCategory().getParent().getName(),
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
            user.getJobTitle(), realtime.isUserOnline(user.getId()), experiences
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

    private boolean isFavorited(Long currentUserId, Long certificationId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM experience_user_library " +
                "WHERE user_id=? AND certification_id=? AND favorited_at IS NOT NULL",
            Integer.class, currentUserId, certificationId
        );
        return count != null && count > 0;
    }

    private record ExperienceCardRow(Long certificationId, Long userId) {}
    private record ExperienceLibraryRow(Long certificationId, Long userId, LocalDateTime occurredAt) {}
    private record LikeTarget(Long userId, String uid, String accountType, String accountStatus, boolean acceptingInquiries) {}
    private record LikeStats(int likeCount, boolean liked) {}

    public record ExperienceLikeView(int likeCount, boolean liked) {}
    public record ExperienceFavoriteView(boolean favorited) {}
    public record ExperienceLibraryItem(AnswererView answerer, LocalDateTime occurredAt) {}
    public record ExperienceLibraryPage(List<ExperienceLibraryItem> items, int page, boolean hasMore) {}

    public record ExperienceView(
        Long certificationId,
        String title,
        String description,
        Integer referenceIndex,
        int likeCount,
        boolean likedByCurrentUser,
        boolean favoritedByCurrentUser,
        String experienceLocation,
        String experienceStartDate,
        String experienceEndDate,
        Integer experienceCount,
        String experienceRole,
        String experienceAgeRange,
        String experienceEducation,
        String experienceJob,
        Long experienceCategoryId,
        Long experienceCategoryParentId,
        String experienceCategoryName,
        String experienceCategoryParentName,
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
        boolean online,
        List<ExperienceView> experiences
    ) {
    }

    public record AnswererPage(List<AnswererView> items, int page, boolean hasMore) {}
}
