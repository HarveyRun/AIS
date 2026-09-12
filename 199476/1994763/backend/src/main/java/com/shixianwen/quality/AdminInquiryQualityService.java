package com.shixianwen.quality;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminInquiryQualityService {
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        return jdbc.queryForMap(
            "SELECT COUNT(*) AS evaluationCount," +
                "SUM(CASE WHEN answered_level=0 OR specific_level=0 OR matched_level=0 " +
                "OR useful_level=0 OR communication_level=0 OR ask_again_level=0 THEN 1 ELSE 0 END) " +
                "AS riskyEvaluationCount FROM inquiry_evaluations"
        );
    }

    @Transactional(readOnly = true)
    public PageResult evaluations(String keyword, String risk, int page, int size) {
        String query = keyword == null ? "" : keyword.trim();
        String like = "%" + query + "%";
        boolean riskyOnly = "RISKY".equalsIgnoreCase(risk);
        String riskSql = riskyOnly
            ? " AND (e.answered_level=0 OR e.specific_level=0 OR e.matched_level=0 " +
                "OR e.useful_level=0 OR e.communication_level=0 OR e.ask_again_level=0)"
            : "";
        String where = " WHERE (?='' OR q.uid LIKE ? OR q.phone LIKE ? OR a.uid LIKE ? OR a.phone LIKE ?)" + riskSql;
        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM inquiry_evaluations e " +
                "JOIN inquiries i ON i.id=e.inquiry_id " +
                "JOIN users q ON q.id=i.questioner_id JOIN users a ON a.id=i.answerer_id" + where,
            Long.class, query, like, like, like, like
        );
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT e.id,e.inquiry_id AS inquiryId,i.question,i.amount," +
                "q.uid AS questionerUid,q.phone AS questionerPhone," +
                "a.uid AS answererUid,a.phone AS answererPhone," +
                "e.answered_level AS answeredLevel,e.specific_level AS specificLevel," +
                "e.matched_level AS matchedLevel,e.useful_level AS usefulLevel," +
                "e.communication_level AS communicationLevel,e.ask_again_level AS askAgainLevel," +
                "e.negative_tags AS negativeTags,e.comment,e.created_at AS createdAt," +
                "((e.answered_level=0)+(e.specific_level=0)+(e.matched_level=0)+" +
                "(e.useful_level=0)+(e.communication_level=0)+(e.ask_again_level=0)) AS issueCount " +
                "FROM inquiry_evaluations e JOIN inquiries i ON i.id=e.inquiry_id " +
                "JOIN users q ON q.id=i.questioner_id JOIN users a ON a.id=i.answerer_id" + where +
                " ORDER BY issueCount DESC,e.id DESC LIMIT ? OFFSET ?",
            query, like, like, like, like, size, page * size
        );
        return new PageResult(items, total == null ? 0 : total, page, size);
    }

    public record PageResult(List<Map<String, Object>> items, long total, int page, int size) {
    }
}
