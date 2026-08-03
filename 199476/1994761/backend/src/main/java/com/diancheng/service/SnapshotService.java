package com.diancheng.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SnapshotService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public SnapshotService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> snapshot(String sessionEmail) {
        boolean admin = AuthService.ADMIN_EMAIL.equals(sessionEmail);
        Map<String, Object> users = new LinkedHashMap<>();
        jdbc.query("SELECT * FROM users ORDER BY created_at", rs -> {
            String email = rs.getString("email");
            boolean privateView = admin || email.equals(sessionEmail);
            String publicKey = "public-" + UUID.nameUUIDFromBytes(email.getBytes(StandardCharsets.UTF_8));
            users.put(privateView ? email : publicKey, user(rs, privateView, sessionEmail));
        });
        List<Map<String, Object>> feedbacks = admin
                ? feedbacks("SELECT * FROM feedbacks ORDER BY updated_at DESC")
                : sessionEmail == null ? List.of() : feedbacks("SELECT * FROM feedbacks WHERE user_email=? ORDER BY updated_at DESC", sessionEmail);
        List<Map<String, Object>> notifications = admin
                ? rows("SELECT * FROM notifications ORDER BY created_at DESC", this::notification)
                : sessionEmail == null ? List.of() : rows("SELECT * FROM notifications WHERE user_email=? ORDER BY created_at DESC", this::notification, sessionEmail);
        List<Map<String, Object>> auditLogs = admin
                ? rows("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 500", this::audit)
                : List.of();
        List<Map<String, Object>> deposits = admin
                ? rows("SELECT * FROM cooperation_deposits ORDER BY created_at DESC", this::deposit)
                : sessionEmail == null ? List.of() : rows("SELECT * FROM cooperation_deposits WHERE user_email=? ORDER BY created_at DESC", this::deposit, sessionEmail);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", users);
        data.put("feedbacks", feedbacks);
        data.put("notifications", notifications);
        data.put("auditLogs", auditLogs);
        data.put("cooperationDeposits", deposits);
        return data;
    }

    private Map<String, Object> user(ResultSet rs, boolean privateView, String sessionEmail) throws SQLException {
        String email = rs.getString("email");
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("email", privateView ? email : "");
        item.put("name", rs.getString("name"));
        item.put("passwordHash", "");
        item.put("inviteCode", privateView ? rs.getString("invite_code") : "");
        item.put("usedInviteCode", privateView ? rs.getString("used_invite_code") : "");
        item.put("createdAt", iso(rs.getTimestamp("created_at")));
        item.put("ideas", privateView
                ? rows("SELECT * FROM ideas WHERE owner_email=? ORDER BY created_at DESC", (ideaRows, row) -> idea(ideaRows, sessionEmail), email)
                : rows("SELECT * FROM ideas WHERE owner_email=? AND type='new' AND is_public=true ORDER BY created_at DESC", (ideaRows, row) -> idea(ideaRows, sessionEmail), email));
        item.put("products", privateView ? rows("SELECT * FROM products WHERE user_email=? ORDER BY created_at DESC", this::product, email) : List.of());
        item.put("teamApplication", privateView ? teamApplication(email) : null);
        item.put("balance", privateView ? decimal(rs.getBigDecimal("balance")) : 0);
        item.put("transactions", privateView ? rows("SELECT * FROM transactions WHERE user_email=? ORDER BY created_at DESC", this::transaction, email) : List.of());
        item.put("packageOrders", privateView ? rows("SELECT * FROM package_orders WHERE user_email=? ORDER BY created_at DESC", this::packageOrder, email) : List.of());
        item.put("activePackage", privateView ? activePackage(email) : null);
        item.put("notifications", List.of());
        return item;
    }

    private Map<String, Object> idea(ResultSet rs, String sessionEmail) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        String id = rs.getString("id");
        item.put("id", id);
        item.put("type", rs.getString("type"));
        item.put("parentId", rs.getString("parent_id"));
        item.put("text", rs.getString("text"));
        item.put("status", rs.getString("status"));
        item.put("level", rs.getObject("level_value") == null ? null : rs.getInt("level_value"));
        item.put("fee", decimal(rs.getBigDecimal("fee")));
        item.put("paid", rs.getBoolean("paid"));
        putNullable(item, "decision", rs.getString("decision"));
        item.put("isPublic", rs.getBoolean("is_public"));
        item.put("likedBy", jdbc.query(
                "SELECT user_email FROM idea_likes WHERE idea_id=? ORDER BY created_at",
                (likes, index) -> {
                    String liker = likes.getString(1);
                    if (liker.equals(sessionEmail)) return liker;
                    return "like-" + UUID.nameUUIDFromBytes(liker.getBytes(StandardCharsets.UTF_8));
                },
                id));
        item.put("createdAt", iso(rs.getTimestamp("created_at")));
        putNullable(item, "updatedAt", iso(rs.getTimestamp("updated_at")));
        putNullable(item, "reviewedAt", iso(rs.getTimestamp("reviewed_at")));
        return item;
    }

    private Map<String, Object> product(ResultSet rs, int row) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getString("id")); item.put("name", rs.getString("name")); item.put("type", rs.getString("type"));
        item.put("summary", rs.getString("summary")); item.put("fileName", rs.getString("file_name")); item.put("size", rs.getLong("size_bytes"));
        putNullable(item, "status", rs.getString("status")); item.put("createdAt", iso(rs.getTimestamp("created_at")));
        return item;
    }

    private Map<String, Object> teamApplication(String email) {
        return jdbc.query("SELECT * FROM team_applications WHERE user_email=?", rs -> {
            if (!rs.next()) return null;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("skill", rs.getString("skill")); putNullable(item, "intro", rs.getString("intro")); item.put("time", rs.getString("available_time"));
            putNullable(item, "resumeId", rs.getString("resume_id")); putNullable(item, "resumeName", rs.getString("resume_name"));
            if (rs.getObject("resume_size") != null) item.put("resumeSize", rs.getLong("resume_size"));
            item.put("status", rs.getString("status")); item.put("createdAt", iso(rs.getTimestamp("created_at"))); putNullable(item, "updatedAt", iso(rs.getTimestamp("updated_at")));
            return item;
        }, email);
    }

    private Map<String, Object> transaction(ResultSet rs, int row) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getString("id")); item.put("type", rs.getString("type")); item.put("title", rs.getString("title")); item.put("amount", decimal(rs.getBigDecimal("amount")));
        putNullable(item, "businessType", rs.getString("business_type")); putNullable(item, "orderId", rs.getString("order_id"));
        putNullable(item, "depositId", rs.getString("deposit_id")); putNullable(item, "payType", rs.getString("pay_type")); item.put("createdAt", iso(rs.getTimestamp("created_at")));
        return item;
    }

    private Map<String, Object> packageOrder(ResultSet rs, int row) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getString("id")); item.put("packageId", rs.getString("package_id")); item.put("packageName", rs.getString("package_name"));
        item.put("levelRange", rs.getString("level_range")); item.put("amount", decimal(rs.getBigDecimal("amount"))); item.put("payType", rs.getString("pay_type"));
        item.put("status", rs.getString("status")); item.put("durationDays", rs.getInt("duration_days")); item.put("projectQuota", rs.getInt("project_quota"));
        item.put("iterationQuota", rs.getInt("iteration_quota")); item.put("benefits", jsonList(rs.getString("benefits")));
        item.put("createdAt", iso(rs.getTimestamp("created_at"))); item.put("activatedAt", iso(rs.getTimestamp("activated_at"))); item.put("expiresAt", iso(rs.getTimestamp("expires_at")));
        return item;
    }

    private Map<String, Object> activePackage(String email) {
        return jdbc.query("SELECT * FROM active_packages WHERE user_email=?", rs -> {
            if (!rs.next()) return null;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("packageId", rs.getString("package_id")); item.put("packageName", rs.getString("package_name")); item.put("levelRange", rs.getString("level_range"));
            item.put("startedAt", iso(rs.getTimestamp("started_at"))); item.put("expiresAt", iso(rs.getTimestamp("expires_at")));
            item.put("projectQuota", rs.getInt("project_quota")); item.put("iterationQuota", rs.getInt("iteration_quota")); item.put("orderId", rs.getString("order_id"));
            return item;
        }, email);
    }

    private List<Map<String, Object>> feedbacks(String sql, Object... args) {
        return rows(sql, (rs, row) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            String id = rs.getString("id"); item.put("id", id); item.put("userEmail", rs.getString("user_email")); item.put("page", rs.getString("page"));
            item.put("category", rs.getString("category")); item.put("status", rs.getString("status")); item.put("createdAt", iso(rs.getTimestamp("created_at")));
            item.put("updatedAt", iso(rs.getTimestamp("updated_at"))); item.put("messages", rows("SELECT * FROM feedback_messages WHERE feedback_id=? ORDER BY created_at", this::feedbackMessage, id));
            return item;
        }, args);
    }

    private Map<String, Object> feedbackMessage(ResultSet rs, int row) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getString("id")); item.put("role", rs.getString("role")); item.put("email", rs.getString("email"));
        item.put("content", rs.getString("content")); item.put("createdAt", iso(rs.getTimestamp("created_at"))); return item;
    }

    private Map<String, Object> notification(ResultSet rs, int row) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getString("id")); item.put("userEmail", rs.getString("user_email")); item.put("type", rs.getString("type"));
        item.put("title", rs.getString("title")); item.put("content", rs.getString("content")); item.put("link", rs.getString("link"));
        item.put("businessId", rs.getString("business_id")); item.put("dedupeKey", rs.getString("dedupe_key")); item.put("read", rs.getBoolean("is_read")); item.put("createdAt", iso(rs.getTimestamp("created_at"))); return item;
    }

    private Map<String, Object> audit(ResultSet rs, int row) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getString("id")); item.put("action", rs.getString("action")); item.put("detail", rs.getString("detail"));
        item.put("actor", rs.getString("actor")); item.put("targetId", rs.getString("target_id")); item.put("createdAt", iso(rs.getTimestamp("created_at"))); return item;
    }

    private Map<String, Object> deposit(ResultSet rs, int row) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getString("id")); item.put("userEmail", rs.getString("user_email")); item.put("amount", decimal(rs.getBigDecimal("amount")));
        item.put("status", rs.getString("status")); item.put("createdAt", iso(rs.getTimestamp("created_at"))); item.put("updatedAt", iso(rs.getTimestamp("updated_at")));
        putNullable(item, "refundedAt", iso(rs.getTimestamp("refunded_at"))); return item;
    }

    private <T> List<T> rows(String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... args) {
        return jdbc.query(sql, mapper, args);
    }

    private List<String> jsonList(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        try { return objectMapper.readValue(value, new TypeReference<>() {}); }
        catch (Exception ignored) { return new ArrayList<>(); }
    }

    private static String iso(Timestamp value) {
        return value == null ? null : value.toLocalDateTime().atOffset(ZoneOffset.ofHours(8)).toInstant().toString();
    }

    private static double decimal(BigDecimal value) { return value == null ? 0 : value.doubleValue(); }
    private static void putNullable(Map<String, Object> map, String key, Object value) { if (value != null) map.put(key, value); }
}
