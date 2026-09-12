package com.shixianwen.inquiry;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InquiryDisputeService {
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public PageResult riskWatch(String keyword, String status, int page, int size) {
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        String cleanStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String like = "%" + cleanKeyword + "%";
        String where = " WHERE (?='' OR w.status=?) AND (?='' OR u.uid LIKE ? OR u.phone LIKE ? OR u.nickname LIKE ?) ";
        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_risk_watchlist w JOIN users u ON u.id=w.user_id" + where,
            Long.class, cleanStatus, cleanStatus, cleanKeyword, like, like, like
        );
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT w.user_id AS userId,u.uid,u.phone,u.nickname,w.risk_score AS riskScore,w.reason,w.status," +
                "w.created_at AS createdAt,w.updated_at AS updatedAt," +
                "GROUP_CONCAT(CONCAT(c.violation_level,':',c.used_count,'/',c.threshold_count) " +
                "ORDER BY c.violation_level SEPARATOR ',') AS counters " +
                "FROM user_risk_watchlist w JOIN users u ON u.id=w.user_id " +
                "LEFT JOIN user_violation_counters c ON c.user_id=u.id AND c.used_count>0" + where +
                "GROUP BY w.user_id,u.uid,u.phone,u.nickname,w.risk_score,w.reason,w.status,w.created_at,w.updated_at " +
                "ORDER BY (w.status='WATCHING') DESC,w.risk_score DESC,w.updated_at DESC LIMIT ? OFFSET ?",
            cleanStatus, cleanStatus, cleanKeyword, like, like, like, size, page * size
        );
        return new PageResult(items, total == null ? 0 : total, page, size);
    }

    @Transactional
    public void dismissRisk(AdminUser admin, Long userId, String reason, String ip) {
        String cleanReason = required(reason, 500, "请填写关注处理说明");
        int updated = jdbc.update(
            "UPDATE user_risk_watchlist SET status='DISMISSED',reason=?,reviewed_by_admin_id=?," +
                "reviewed_at=NOW(6) WHERE user_id=?",
            cleanReason, admin.getId(), userId
        );
        if (updated != 1) throw BusinessException.notFound("风险关注记录不存在");
        jdbc.update(
            "INSERT INTO admin_audit_logs(admin_user_id,action,target_type,target_id,detail,ip_address) " +
                "VALUES(?,?,?,?,?,?)",
            admin.getId(), "DISMISS_RISK_WATCH", "USER", String.valueOf(userId), cleanReason, ip
        );
    }

    private String required(String value, int max, String message) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty()) throw BusinessException.badRequest(message);
        return result.substring(0, Math.min(result.length(), max));
    }

    public record PageResult(List<Map<String, Object>> items, long total, int page, int size) {
    }
}
