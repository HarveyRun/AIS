package com.shixianwen.discovery;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shixianwen.common.BusinessException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiscoveryService {
    private static final String QUALIFIED_USER =
        "EXISTS (SELECT 1 FROM certifications ci WHERE ci.user_id=u.id AND ci.certification_type='IDENTITY' AND ci.status='APPROVED' AND ci.enabled=TRUE AND ci.deleted_at IS NULL) " +
        "AND EXISTS (SELECT 1 FROM certifications cj WHERE cj.user_id=u.id AND cj.certification_type='MAIN_JOB' AND cj.status='APPROVED' AND cj.enabled=TRUE AND cj.deleted_at IS NULL) ";
    private static final Map<String, String> MAIN_CATEGORIES = Map.of(
        "GENERAL", "通用",
        "LIFE", "生活",
        "WORK", "工作",
        "ENTERTAINMENT", "娱乐"
    );
    private static final List<String> MAIN_ORDER = List.of("GENERAL", "LIFE", "WORK", "ENTERTAINMENT");
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public MainCategoryView category(String mainCategory, ContentType content) {
        String selectedCode = mainCategory == null ? "" : mainCategory.trim().toUpperCase();
        if (!MAIN_CATEGORIES.containsKey(selectedCode)) throw BusinessException.badRequest("分类不存在");
        List<Map<String, Object>> categories = jdbc.queryForList(
            "SELECT id,main_category AS mainCategory,name FROM discovery_categories WHERE active=TRUE AND deleted_at IS NULL AND main_category=? AND content_scope IN ('BOTH',?) ORDER BY sort_order,id",
            selectedCode, content.name()
        );
        List<Map<String, Object>> matters = content == ContentType.MATTERS ? jdbc.queryForList(
            "SELECT m.id,m.category_id AS categoryId,m.title FROM discovery_matters m JOIN discovery_categories c ON c.id=m.category_id WHERE m.active=TRUE AND m.deleted_at IS NULL AND c.deleted_at IS NULL AND c.main_category=? ORDER BY m.sort_order,m.id",
            selectedCode
        ) : List.of();
        List<Map<String, Object>> participants = List.of();
        List<Map<String, Object>> matterJobs = List.of();
        List<Map<String, Object>> experiences = content == ContentType.EXPERIENCES ? jdbc.queryForList(
            "SELECT e.id,e.category_id AS categoryId,e.name AS title,COUNT(DISTINCT u.id) AS answererCount " +
                "FROM discovery_experiences e JOIN discovery_categories dc ON dc.id=e.category_id " +
                "JOIN certifications c ON c.discovery_experience_id=e.id AND c.category='EXPERIENCE' AND c.status='APPROVED' AND c.enabled=TRUE AND c.deleted_at IS NULL " +
                "JOIN users u ON u.id=c.user_id " +
                "WHERE e.active=TRUE AND e.deleted_at IS NULL AND dc.active=TRUE AND dc.deleted_at IS NULL AND dc.main_category=? " +
                "AND u.account_status='ACTIVE' AND u.accepting_inquiries=TRUE AND " + QUALIFIED_USER +
                "GROUP BY e.id,e.category_id,e.name ORDER BY e.name",
            selectedCode
        ) : List.of();

        Map<Long, List<ParticipantView>> participantMap = new LinkedHashMap<>();
        for (Map<String, Object> row : participants) {
            long matterId = ((Number) row.get("matterId")).longValue();
            participantMap.computeIfAbsent(matterId, ignored -> new ArrayList<>()).add(new ParticipantView(
                String.valueOf(row.get("uid")),
                String.valueOf(row.get("jobNames"))
            ));
        }
        Map<Long, List<JobView>> jobMap = new LinkedHashMap<>();
        for (Map<String, Object> row : matterJobs) {
            long matterId = ((Number) row.get("matterId")).longValue();
            jobMap.computeIfAbsent(matterId, ignored -> new ArrayList<>()).add(new JobView(
                ((Number) row.get("jobId")).longValue(), String.valueOf(row.get("name")),
                ((Number) row.get("answererCount")).longValue()
            ));
        }
        Map<Long, List<MatterView>> matterMap = new LinkedHashMap<>();
        for (Map<String, Object> row : matters) {
            long categoryId = ((Number) row.get("categoryId")).longValue();
            long matterId = ((Number) row.get("id")).longValue();
            matterMap.computeIfAbsent(categoryId, ignored -> new ArrayList<>())
                .add(new MatterView(matterId, String.valueOf(row.get("title")), jobMap.getOrDefault(matterId, List.of()), participantMap.getOrDefault(matterId, List.of())));
        }
        Map<Long, List<ExperienceView>> experienceMap = new LinkedHashMap<>();
        for (Map<String, Object> row : experiences) {
            long categoryId = ((Number) row.get("categoryId")).longValue();
            experienceMap.computeIfAbsent(categoryId, ignored -> new ArrayList<>())
                .add(new ExperienceView(((Number) row.get("id")).longValue(), String.valueOf(row.get("title")), ((Number) row.get("answererCount")).longValue()));
        }

        Map<String, List<SubcategoryView>> grouped = new LinkedHashMap<>();
        grouped.put(selectedCode, new ArrayList<>());
        for (Map<String, Object> row : categories) {
            long id = ((Number) row.get("id")).longValue();
            String code = String.valueOf(row.get("mainCategory"));
            grouped.computeIfAbsent(code, ignored -> new ArrayList<>()).add(new SubcategoryView(
                id,
                String.valueOf(row.get("name")),
                matterMap.getOrDefault(id, List.of()),
                experienceMap.getOrDefault(id, List.of())
            ));
        }
        List<MainCategoryView> result = List.of(new MainCategoryView(
            selectedCode, MAIN_CATEGORIES.get(selectedCode), grouped.getOrDefault(selectedCode, List.of())
        ));
        return result.get(0);
    }

    @Transactional(readOnly = true)
    public MatterView matter(Long id) {
        Map<String, Object> row = jdbc.queryForList(
            "SELECT id,title FROM discovery_matters WHERE id=? AND active=TRUE AND deleted_at IS NULL", id
        ).stream().findFirst().orElseThrow(() -> BusinessException.notFound("事情不存在"));
        List<JobView> jobs = jdbc.query(
            "SELECT j.id,j.name," +
                "COUNT(DISTINCT CASE WHEN uj.verified=TRUE AND u.account_status='ACTIVE' AND u.accepting_inquiries=TRUE AND " + QUALIFIED_USER + "THEN u.id END) AS answererCount " +
                "FROM discovery_matter_jobs mj JOIN jobs j ON j.id=mj.job_id " +
                "LEFT JOIN user_jobs uj ON uj.job_id=j.id AND uj.deleted_at IS NULL LEFT JOIN users u ON u.id=uj.user_id " +
                "WHERE mj.matter_id=? AND mj.active=TRUE AND mj.deleted_at IS NULL AND j.active=TRUE AND j.deleted_at IS NULL GROUP BY j.id,j.name,mj.sort_order " +
                "ORDER BY mj.sort_order,j.name",
            (rs, index) -> new JobView(rs.getLong("id"), rs.getString("name"), rs.getLong("answererCount")), id
        );
        List<ParticipantView> participants = jdbc.query(
            "SELECT u.uid," +
                "GROUP_CONCAT(DISTINCT j.name ORDER BY j.name SEPARATOR '、') AS jobNames " +
                "FROM discovery_matter_jobs mj JOIN user_jobs uj ON uj.job_id=mj.job_id JOIN jobs j ON j.id=mj.job_id JOIN users u ON u.id=uj.user_id " +
                "WHERE mj.matter_id=? AND mj.active=TRUE AND mj.deleted_at IS NULL AND uj.deleted_at IS NULL AND j.active=TRUE AND j.deleted_at IS NULL AND uj.verified=TRUE AND u.account_status='ACTIVE' AND u.accepting_inquiries=TRUE AND " + QUALIFIED_USER +
                "GROUP BY u.id,u.uid ORDER BY u.id",
            (rs, index) -> new ParticipantView(rs.getString("uid"), rs.getString("jobNames")), id
        );
        return new MatterView(id, String.valueOf(row.get("title")), jobs, participants);
    }

    @Transactional(readOnly = true)
    public List<MatterSearchView> searchMatters(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);

        if (normalizedKeyword.isEmpty()) {
            return jdbc.query(
                "SELECT m.id,m.title,c.id AS categoryId,c.name AS categoryName FROM discovery_matters m " +
                    "JOIN discovery_categories c ON c.id=m.category_id " +
                "WHERE m.active=TRUE AND m.deleted_at IS NULL AND c.active=TRUE AND c.deleted_at IS NULL " +
                    "ORDER BY FIELD(c.main_category,'GENERAL','LIFE','WORK','ENTERTAINMENT')," +
                    "c.sort_order,c.id,m.sort_order,m.id",
                (resultSet, rowNumber) -> new MatterSearchView(
                    resultSet.getLong("id"),
                    resultSet.getString("title"),
                    resultSet.getLong("categoryId"),
                    resultSet.getString("categoryName")
                )
            );
        }

        return jdbc.query(
            "SELECT m.id,m.title,c.id AS categoryId,c.name AS categoryName FROM discovery_matters m " +
                "JOIN discovery_categories c ON c.id=m.category_id " +
                "WHERE m.active=TRUE AND m.deleted_at IS NULL AND c.active=TRUE AND c.deleted_at IS NULL AND m.title LIKE CONCAT('%',?,'%') " +
                "ORDER BY FIELD(c.main_category,'GENERAL','LIFE','WORK','ENTERTAINMENT')," +
                "c.sort_order,c.id,m.sort_order,m.id LIMIT 50",
            (resultSet, rowNumber) -> new MatterSearchView(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getLong("categoryId"),
                resultSet.getString("categoryName")
            ),
            normalizedKeyword
        );
    }

    @Transactional(readOnly = true)
    public List<ExperienceSearchView> searchExperiences(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);

        if (normalizedKeyword.isEmpty()) {
            return jdbc.query(
                "SELECT e.id,e.category_id AS categoryId,dc.name AS categoryName,e.name AS title,COUNT(DISTINCT u.id) AS answererCount " +
                    "FROM discovery_experiences e JOIN discovery_categories dc ON dc.id=e.category_id " +
                    "JOIN certifications c ON c.discovery_experience_id=e.id AND c.category='EXPERIENCE' AND c.status='APPROVED' AND c.enabled=TRUE AND c.deleted_at IS NULL " +
                    "JOIN users u ON u.id=c.user_id " +
                    "WHERE e.active=TRUE AND e.deleted_at IS NULL AND dc.active=TRUE AND dc.deleted_at IS NULL " +
                    "AND u.account_status='ACTIVE' AND u.accepting_inquiries=TRUE AND " + QUALIFIED_USER +
                    "GROUP BY e.id,e.category_id,dc.name,e.name " +
                    "ORDER BY FIELD(dc.main_category,'GENERAL','LIFE','WORK','ENTERTAINMENT')," +
                    "dc.sort_order,dc.id,e.name",
                (resultSet, rowNumber) -> new ExperienceSearchView(
                    resultSet.getLong("id"), resultSet.getLong("categoryId"),
                    resultSet.getString("categoryName"),
                    resultSet.getString("title"),
                    resultSet.getLong("answererCount")
                )
            );
        }

        return jdbc.query(
            "SELECT e.id,e.category_id AS categoryId,dc.name AS categoryName,e.name AS title,COUNT(DISTINCT u.id) AS answererCount " +
                "FROM discovery_experiences e JOIN discovery_categories dc ON dc.id=e.category_id " +
                "JOIN certifications c ON c.discovery_experience_id=e.id AND c.category='EXPERIENCE' AND c.status='APPROVED' AND c.enabled=TRUE AND c.deleted_at IS NULL " +
                "JOIN users u ON u.id=c.user_id " +
                "WHERE e.active=TRUE AND e.deleted_at IS NULL AND dc.active=TRUE AND dc.deleted_at IS NULL AND e.name LIKE CONCAT('%',?,'%') " +
                "AND u.account_status='ACTIVE' AND u.accepting_inquiries=TRUE AND " + QUALIFIED_USER +
                "GROUP BY e.id,e.category_id,dc.name,e.name " +
                "ORDER BY FIELD(dc.main_category,'GENERAL','LIFE','WORK','ENTERTAINMENT')," +
                "dc.sort_order,dc.id,e.name LIMIT 50",
            (resultSet, rowNumber) -> new ExperienceSearchView(
                resultSet.getLong("id"), resultSet.getLong("categoryId"),
                resultSet.getString("categoryName"),
                resultSet.getString("title"),
                resultSet.getLong("answererCount")
            ),
            normalizedKeyword
        );
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) return "";
        String normalized = keyword.trim();
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
    }

    public enum ContentType { MATTERS, EXPERIENCES }

    public record CatalogView(List<MainCategoryView> categories) {}
    public record MainCategoryView(String code, String name, List<SubcategoryView> subcategories) {}
    public record SubcategoryView(Long id, String name, List<MatterView> matters, List<ExperienceView> experiences) {}
    public record MatterView(Long id, String title, List<JobView> jobs, List<ParticipantView> participants) {}
    public record JobView(Long id, String name, long answererCount) {}
    public record ParticipantView(String uid, String jobNames) {}
    public record ExperienceView(Long id, String title, long answererCount) {}
    public record MatterSearchView(Long id, String title, Long categoryId, String categoryName) {}
    public record ExperienceSearchView(Long id, Long categoryId, String categoryName, String title, long answererCount) {}
}
