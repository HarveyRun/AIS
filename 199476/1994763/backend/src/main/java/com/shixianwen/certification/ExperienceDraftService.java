package com.shixianwen.certification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExperienceDraftService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public DraftView get(Long userId, String key) {
        String safeKey = normalizeKey(key);
        return jdbc.query(
            "SELECT content_json,updated_at FROM experience_form_drafts WHERE user_id=? AND draft_key=?",
            (rs, rowNum) -> new DraftView(
                true,
                readContent(rs.getString("content_json")),
                rs.getTimestamp("updated_at").toLocalDateTime()
            ),
            userId, safeKey
        ).stream().findFirst().orElseGet(() -> new DraftView(false, Map.of(), null));
    }

    @Transactional(readOnly = true)
    public List<DraftItem> list(Long userId) {
        return jdbc.query(
            "SELECT draft_key,content_json,updated_at FROM experience_form_drafts " +
                "WHERE user_id=? ORDER BY updated_at DESC,id DESC",
            (rs, rowNum) -> new DraftItem(
                rs.getString("draft_key"),
                readContent(rs.getString("content_json")),
                rs.getTimestamp("updated_at").toLocalDateTime()
            ),
            userId
        );
    }

    @Transactional
    public DraftView save(Long userId, String key, Map<String, Object> content) {
        String safeKey = normalizeKey(key);
        Map<String, Object> safeContent = content == null ? Map.of() : content;
        String json;
        try {
            json = objectMapper.writeValueAsString(safeContent);
        } catch (Exception exception) {
            throw BusinessException.badRequest("草稿内容无法保存");
        }
        if (json.length() > 32_000) throw BusinessException.badRequest("草稿内容过长");
        jdbc.update(
            "INSERT INTO experience_form_drafts(user_id,draft_key,content_json,updated_at) " +
                "VALUES (?,?,?,CURRENT_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE " +
                "content_json=VALUES(content_json),updated_at=CURRENT_TIMESTAMP(6)",
            userId, safeKey, json
        );
        return get(userId, safeKey);
    }

    @Transactional
    public void delete(Long userId, String key) {
        jdbc.update(
            "DELETE FROM experience_form_drafts WHERE user_id=? AND draft_key=?",
            userId, normalizeKey(key)
        );
    }

    private Map<String, Object> readContent(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private String normalizeKey(String key) {
        String value = key == null ? "CREATE" : key.trim().toUpperCase();
        if (!value.matches("(?:CREATE|EDIT_[0-9]{1,18})")) {
            throw BusinessException.badRequest("草稿标识无效");
        }
        return value;
    }

    public record DraftView(boolean exists, Map<String, Object> content, LocalDateTime updatedAt) {}
    public record DraftItem(String key, Map<String, Object> content, LocalDateTime updatedAt) {}
}
