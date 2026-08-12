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
            "SELECT u.id FROM users u WHERE u.id<>? AND u.account_status='ACTIVE' AND u.answerer_status='APPROVED' AND u.accepting_inquiries=TRUE " +
                "AND (?='' OR COALESCE(u.nickname,'') LIKE CONCAT('%',?,'%') " +
                "OR EXISTS (SELECT 1 FROM user_jobs uj JOIN jobs j ON j.id=uj.job_id WHERE uj.user_id=u.id AND uj.verified=TRUE AND j.name LIKE CONCAT('%',?,'%')) " +
                "OR EXISTS (SELECT 1 FROM certifications c WHERE c.user_id=u.id AND c.status='APPROVED' AND (c.title LIKE CONCAT('%',?,'%') OR COALESCE(c.description,'') LIKE CONCAT('%',?,'%')))) " +
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
            .filter(item -> "APPROVED".equals(item.getAnswererStatus()))
            .orElseThrow(() -> BusinessException.notFound("该答主不存在"));
        return toView(user);
    }

    private AnswererView toView(User user) {
        List<Certification> certifications =
            certificationRepository.findByUserIdAndStatusOrderByIdAsc(user.getId(), "APPROVED");
        List<JobView> jobs = jdbc.query(
            "SELECT j.name,uj.capability_description,c.years FROM user_jobs uj JOIN jobs j ON j.id=uj.job_id LEFT JOIN certifications c ON c.id=uj.certification_id WHERE uj.user_id=? AND uj.verified=TRUE AND j.active=TRUE ORDER BY CASE WHEN uj.certification_id IS NOT NULL THEN 0 ELSE 1 END,uj.certification_id DESC,j.name LIMIT 1",
            (rs, row) -> new JobView(rs.getString("name"), (Integer) rs.getObject("years"), rs.getString("capability_description")),
            user.getId()
        );
        JobView job = jobs.isEmpty() ? null : jobs.get(0);
        List<ExperienceView> experiences = certifications.stream()
            .filter(item -> "EXPERIENCE".equals(item.getCategory()))
            .map(item -> new ExperienceView(
                item.getId(), item.getTitle(), item.getDescription(), item.getYears(), item.getDiscoveryCategoryId()
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
                "WHERE mj.matter_id=? AND uj.verified=TRUE AND u.account_status='ACTIVE' " +
                "AND u.answerer_status='APPROVED' AND u.accepting_inquiries=TRUE ORDER BY u.id DESC",
            Long.class, matterId
        );
        return ids.stream().map(userRepository::findById).flatMap(java.util.Optional::stream).map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<AnswererView> forExperience(Long categoryId, String title, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<Long> ids = jdbc.queryForList(
            "SELECT DISTINCT u.id FROM certifications c JOIN users u ON u.id=c.user_id " +
                "LEFT JOIN user_jobs uj ON uj.user_id=u.id AND uj.verified=TRUE LEFT JOIN jobs j ON j.id=uj.job_id " +
                "WHERE c.category='EXPERIENCE' AND c.status='APPROVED' AND c.discovery_category_id=? AND c.title=? " +
                "AND u.account_status='ACTIVE' AND u.answerer_status='APPROVED' AND u.accepting_inquiries=TRUE " +
                "AND (?='' OR COALESCE(u.nickname,'') LIKE CONCAT('%',?,'%') OR COALESCE(j.name,'') LIKE CONCAT('%',?,'%')) " +
                "ORDER BY u.id DESC",
            Long.class, categoryId, title, normalizedKeyword, normalizedKeyword, normalizedKeyword
        );
        return ids.stream().map(userRepository::findById).flatMap(java.util.Optional::stream).map(this::toView).toList();
    }

    private record JobView(String name, Integer years, String description) {}

    public record ExperienceView(
        Long certificationId,
        String title,
        String description,
        Integer years,
        Long discoveryCategoryId
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
