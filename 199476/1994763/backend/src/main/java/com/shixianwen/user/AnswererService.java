package com.shixianwen.user;

import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Service
public class AnswererService {
    private static final String QUALIFIED =
        "EXISTS (SELECT 1 FROM certifications ci WHERE ci.user_id=u.id AND ci.certification_type='IDENTITY' AND ci.status='APPROVED' AND ci.enabled=TRUE AND ci.deleted_at IS NULL) " +
        "AND EXISTS (SELECT 1 FROM certifications cj WHERE cj.user_id=u.id AND cj.certification_type='MAIN_JOB' AND cj.status='APPROVED' AND cj.enabled=TRUE AND cj.deleted_at IS NULL) ";
    private final UserRepository userRepository;
    private final CertificationRepository certificationRepository;
    private final JdbcTemplate jdbc;

    public AnswererService(UserRepository userRepository, CertificationRepository certificationRepository, JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.certificationRepository = certificationRepository;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public AnswererPage search(Long currentUserId, String keyword, int page, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        List<Long> ids = jdbc.queryForList(
            "SELECT u.id FROM users u WHERE u.id<>? AND u.account_status='ACTIVE' AND u.accepting_inquiries=TRUE AND " + QUALIFIED +
                "AND (?='' OR COALESCE(u.nickname,'') LIKE CONCAT('%',?,'%') " +
                "OR EXISTS (SELECT 1 FROM user_jobs uj JOIN jobs j ON j.id=uj.job_id WHERE uj.user_id=u.id AND uj.verified=TRUE AND uj.deleted_at IS NULL AND j.active=TRUE AND j.deleted_at IS NULL AND j.name LIKE CONCAT('%',?,'%')) " +
                "OR EXISTS (SELECT 1 FROM certifications c WHERE c.user_id=u.id AND c.status='APPROVED' AND c.enabled=TRUE AND c.deleted_at IS NULL AND (c.title LIKE CONCAT('%',?,'%') OR COALESCE(c.description,'') LIKE CONCAT('%',?,'%')))) " +
                "ORDER BY u.id DESC LIMIT ? OFFSET ?",
            Long.class, currentUserId, normalizedKeyword, normalizedKeyword, normalizedKeyword, normalizedKeyword, normalizedKeyword,
            safeSize + 1, safePage * safeSize
        );
        boolean hasMore = ids.size() > safeSize;
        List<AnswererView> items = ids.stream().limit(safeSize).map(userRepository::findById)
            .flatMap(java.util.Optional::stream).map(this::toView).toList();
        return new AnswererPage(items, safePage, hasMore);
    }

    @Transactional(readOnly = true)
    public AnswererView detail(String uid) {
        User user = userRepository.findByUidAndAccountStatus(uid, "ACTIVE")
            .filter(item -> currentQualification(item.getId()))
            .orElseThrow(() -> BusinessException.notFound("该答主不存在"));
        return toView(user);
    }

    private AnswererView toView(User user) {
        List<Certification> certifications =
            certificationRepository.findByUserIdAndStatusAndEnabledTrueOrderByIdAsc(user.getId(), "APPROVED");
        List<JobView> jobs = jdbc.query(
            "SELECT j.name,uj.capability_description,c.years FROM user_jobs uj JOIN jobs j ON j.id=uj.job_id LEFT JOIN certifications c ON c.id=uj.certification_id WHERE uj.user_id=? AND uj.verified=TRUE AND uj.deleted_at IS NULL AND j.active=TRUE AND j.deleted_at IS NULL AND (c.id IS NULL OR (c.enabled=TRUE AND c.deleted_at IS NULL)) ORDER BY CASE WHEN uj.certification_id IS NOT NULL THEN 0 ELSE 1 END,uj.certification_id DESC,j.name LIMIT 1",
            (rs, row) -> new JobView(rs.getString("name"), (Integer) rs.getObject("years"), rs.getString("capability_description")),
            user.getId()
        );
        JobView job = jobs.isEmpty() ? null : jobs.get(0);
        List<ExperienceView> experiences = certifications.stream()
            .filter(item -> "EXPERIENCE".equals(item.getCategory()))
            .map(item -> new ExperienceView(
                item.getId(), item.getTitle(), item.getDescription(), item.getYears(), item.getDiscoveryCategoryId(), item.getDiscoveryExperienceId()
            ))
            .toList();
        return new AnswererView(
            user.getId(), user.getUid(), user.getNickname(), user.getAvatarUrl(), user.isAcceptingInquiries(),
            job == null ? null : job.name(), job == null ? null : job.years(), job == null ? null : job.description(), experiences
        );
    }

    @Transactional(readOnly = true)
    public List<AnswererView> forMatter(Long matterId) {
        List<Long> ids = jdbc.queryForList(
            "SELECT DISTINCT u.id FROM discovery_matter_jobs mj " +
                "JOIN user_jobs uj ON uj.job_id=mj.job_id JOIN users u ON u.id=uj.user_id " +
                "JOIN jobs j ON j.id=uj.job_id WHERE mj.matter_id=? AND mj.active=TRUE AND mj.deleted_at IS NULL AND uj.deleted_at IS NULL AND j.active=TRUE AND j.deleted_at IS NULL AND uj.verified=TRUE AND u.account_status='ACTIVE' " +
                "AND u.accepting_inquiries=TRUE AND " + QUALIFIED + "ORDER BY u.id DESC",
            Long.class, matterId
        );
        return ids.stream().map(userRepository::findById).flatMap(java.util.Optional::stream).map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<AnswererView> forExperience(Long experienceId) {
        List<Long> ids = jdbc.queryForList(
            "SELECT DISTINCT u.id FROM certifications c JOIN users u ON u.id=c.user_id " +
                "JOIN discovery_experiences e ON e.id=c.discovery_experience_id " +
                "WHERE c.category='EXPERIENCE' AND c.status='APPROVED' AND c.enabled=TRUE AND c.deleted_at IS NULL AND c.discovery_experience_id=? AND e.active=TRUE AND e.deleted_at IS NULL " +
                "AND u.account_status='ACTIVE' AND u.accepting_inquiries=TRUE AND " + QUALIFIED +
                "ORDER BY u.id DESC",
            Long.class, experienceId
        );
        return ids.stream().map(userRepository::findById).flatMap(java.util.Optional::stream).map(this::toView).toList();
    }

    private boolean currentQualification(Long userId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users u WHERE u.id=? AND " + QUALIFIED,
            Integer.class,
            userId
        );
        return count != null && count > 0;
    }

    private record JobView(String name, Integer years, String description) {}

    public record ExperienceView(
        Long certificationId,
        String title,
        String description,
        Integer years,
        Long discoveryCategoryId,
        Long discoveryExperienceId
    ) {
    }

    public record AnswererView(
        Long id,
        String uid,
        String nickname,
        String avatarUrl,
        boolean acceptingInquiries,
        String mainJob,
        Integer mainJobYears,
        String capabilityDescription,
        List<ExperienceView> experiences
    ) {
    }

    public record AnswererPage(List<AnswererView> items, int page, boolean hasMore) {}
}
