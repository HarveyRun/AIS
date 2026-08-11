package com.shixianwen.discovery;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiscoveryService {
    private static final Map<String, String> MAIN_CATEGORIES = Map.of(
        "LIFE", "生活",
        "WORK", "工作",
        "ENTERTAINMENT", "娱乐"
    );
    private static final List<String> MAIN_ORDER = List.of("LIFE", "WORK", "ENTERTAINMENT");
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public CatalogView catalog() {
        List<Map<String, Object>> categories = jdbc.queryForList(
            "SELECT id,main_category AS mainCategory,name FROM discovery_categories WHERE active=TRUE ORDER BY FIELD(main_category,'LIFE','WORK','ENTERTAINMENT'),sort_order,id"
        );
        List<Map<String, Object>> matters = jdbc.queryForList(
            "SELECT id,category_id AS categoryId,title FROM discovery_matters WHERE active=TRUE ORDER BY sort_order,id"
        );
        List<Map<String, Object>> participants = jdbc.queryForList(
            "SELECT p.matter_id AS matterId,u.uid,p.participation_type AS type FROM discovery_matter_participants p JOIN users u ON u.id=p.user_id " +
                "WHERE u.account_status='ACTIVE' AND u.answerer_status='APPROVED' AND u.accepting_inquiries=TRUE " +
                "ORDER BY p.matter_id,CASE p.participation_type WHEN 'PRIMARY' THEN 0 ELSE 1 END,p.sort_order,u.id"
        );
        List<Map<String, Object>> experiences = jdbc.queryForList(
            "SELECT c.discovery_category_id AS categoryId,c.title,COUNT(*) AS answererCount " +
                "FROM certifications c JOIN users u ON u.id=c.user_id " +
                "WHERE c.category='EXPERIENCE' AND c.status='APPROVED' AND c.discovery_category_id IS NOT NULL " +
                "AND u.account_status='ACTIVE' AND u.answerer_status='APPROVED' AND u.accepting_inquiries=TRUE " +
                "GROUP BY c.discovery_category_id,c.title ORDER BY c.title"
        );

        Map<Long, List<ParticipantView>> participantMap = new LinkedHashMap<>();
        for (Map<String, Object> row : participants) {
            long matterId = ((Number) row.get("matterId")).longValue();
            participantMap.computeIfAbsent(matterId, ignored -> new ArrayList<>()).add(new ParticipantView(
                String.valueOf(row.get("uid")),
                String.valueOf(row.get("type"))
            ));
        }
        Map<Long, List<MatterView>> matterMap = new LinkedHashMap<>();
        for (Map<String, Object> row : matters) {
            long categoryId = ((Number) row.get("categoryId")).longValue();
            long matterId = ((Number) row.get("id")).longValue();
            matterMap.computeIfAbsent(categoryId, ignored -> new ArrayList<>())
                .add(new MatterView(matterId, String.valueOf(row.get("title")), participantMap.getOrDefault(matterId, List.of())));
        }
        Map<Long, List<ExperienceView>> experienceMap = new LinkedHashMap<>();
        for (Map<String, Object> row : experiences) {
            long categoryId = ((Number) row.get("categoryId")).longValue();
            experienceMap.computeIfAbsent(categoryId, ignored -> new ArrayList<>())
                .add(new ExperienceView(String.valueOf(row.get("title")), ((Number) row.get("answererCount")).longValue()));
        }

        Map<String, List<SubcategoryView>> grouped = new LinkedHashMap<>();
        for (String code : MAIN_ORDER) grouped.put(code, new ArrayList<>());
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
        List<MainCategoryView> result = MAIN_ORDER.stream()
            .map(code -> new MainCategoryView(code, MAIN_CATEGORIES.get(code), grouped.getOrDefault(code, List.of())))
            .toList();
        return new CatalogView(result);
    }

    public record CatalogView(List<MainCategoryView> categories) {}
    public record MainCategoryView(String code, String name, List<SubcategoryView> subcategories) {}
    public record SubcategoryView(Long id, String name, List<MatterView> matters, List<ExperienceView> experiences) {}
    public record MatterView(Long id, String title, List<ParticipantView> participants) {}
    public record ParticipantView(String uid, String type) {}
    public record ExperienceView(String title, long answererCount) {}
}
