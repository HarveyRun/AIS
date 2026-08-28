package com.shixianwen.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.common.BusinessException;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsEventService {
    private static final int MAX_BATCH_SIZE = 50;
    private static final int MAX_PROPERTIES = 24;
    private static final Set<String> ALLOWED_CLIENT_EVENTS = Set.of(
        "app_open", "page_view", "login_page_view", "login_submit", "logout_success",
        "home_view", "banner_impression", "banner_click", "home_search_submit",
        "answerer_card_impression", "answerer_card_click", "matter_entry_click",
        "matter_select", "experience_entry_click", "experience_select", "job_filter_select",
        "people_result_view", "people_result_empty", "search_submit", "profile_view",
        "inquiry_start", "inquiry_submit_click", "chat_open", "image_preview",
        "certification_home_view", "job_certification_start", "job_method_select",
        "experience_create_start", "recharge_start", "alipay_payment_launch",
        "alipay_authorization_start", "invitation_rules_view", "faq_view",
        "customer_service_open", "update_prompt_view", "update_action_click"
    );

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final SensitiveWordService sensitiveWords;

    @Value("${app.analytics.environment:prod}")
    private String environment;

    /**
     * Builds analytics properties without allowing an optional metric to break
     * the business transaction. Null values are intentionally omitted.
     */
    public Map<String, Object> properties(Object... keyValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (keyValues == null) return result;
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            Object key = keyValues[index];
            Object value = keyValues[index + 1];
            if (key != null && value != null) result.put(String.valueOf(key), value);
        }
        return result;
    }

    public IngestResult ingest(User user, ClientBatch batch) {
        if (batch == null || batch.events() == null || batch.events().isEmpty()) {
            throw BusinessException.badRequest("埋点数据不能为空");
        }
        if (batch.events().size() > MAX_BATCH_SIZE) {
            throw BusinessException.badRequest("单次最多上报50条事件");
        }
        String platform = normalizePlatform(batch.platform());
        int inserted = 0;
        for (ClientEvent event : batch.events()) {
            validateClientEvent(event);
            LocalDateTime occurredAt = normalizeTime(event.occurredAt());
            int changed = insert(
                event.eventId(), event.eventName(), event.eventVersion(), "CLIENT", user,
                clean(batch.anonymousId(), 80), clean(event.sessionId(), 80), platform,
                clean(batch.appVersion(), 30), clean(event.pageName(), 100),
                clean(event.sourcePage(), 100), sanitizeProperties(event.properties()), occurredAt
            );
            inserted += changed;
        }
        updateIngestion(platform, batch.events().size(), inserted);
        return new IngestResult(batch.events().size(), inserted, batch.events().size() - inserted);
    }

    public void recordBusinessAfterCommit(User user, String eventName, Map<String, ?> properties) {
        Runnable action = () -> recordBusinessSafely(user, eventName, properties);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    public void recordBusinessSafely(User user, String eventName, Map<String, ?> properties) {
        try {
            String platform = property(properties, "platform", "server");
            insert(
                UUID.randomUUID().toString(), clean(eventName, 80), 1, "SERVER", user,
                null, null, normalizePlatform(platform), property(properties, "app_version", null),
                property(properties, "page_name", null), property(properties, "source_page", null),
                sanitizeProperties(properties), LocalDateTime.now()
            );
        } catch (Exception exception) {
            log.warn("Failed to record analytics event {}: {}", eventName, exception.getMessage());
        }
    }

    private int insert(
        String eventId,
        String eventName,
        int eventVersion,
        String sourceType,
        User user,
        String anonymousId,
        String sessionId,
        String platform,
        String appVersion,
        String pageName,
        String sourcePage,
        Map<String, Object> properties,
        LocalDateTime occurredAt
    ) {
        return jdbc.update(
            "INSERT IGNORE INTO analytics_events(" +
                "event_id,event_name,event_version,source_type,user_id,anonymous_id,session_id," +
                "platform,app_version,page_name,source_page,environment,test_account,properties,occurred_at" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            eventId, eventName, Math.max(eventVersion, 1), sourceType,
            user == null ? null : user.getId(), anonymousId, sessionId, platform,
            appVersion, pageName, sourcePage, normalizeEnvironment(),
            user != null && "TEST".equals(user.getAccountType()), toJson(properties), occurredAt
        );
    }

    private void updateIngestion(String platform, int attempted, int inserted) {
        jdbc.update(
            "INSERT INTO analytics_ingestion_daily(" +
                "metric_date,environment,platform,attempted_count,accepted_count,duplicate_count" +
                ") VALUES (CURRENT_DATE,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                "attempted_count=attempted_count+VALUES(attempted_count)," +
                "accepted_count=accepted_count+VALUES(accepted_count)," +
                "duplicate_count=duplicate_count+VALUES(duplicate_count)",
            normalizeEnvironment(), platform, attempted, inserted, attempted - inserted
        );
    }

    private void validateClientEvent(ClientEvent event) {
        if (event == null || event.eventId() == null ||
            !event.eventId().matches("[A-Za-z0-9_-]{16,64}")) {
            throw BusinessException.badRequest("事件编号不正确");
        }
        if (!ALLOWED_CLIENT_EVENTS.contains(event.eventName())) {
            throw BusinessException.badRequest("不支持的埋点事件");
        }
    }

    private LocalDateTime normalizeTime(String value) {
        if (value == null || value.isBlank()) return LocalDateTime.now();
        try {
            LocalDateTime parsed = java.time.OffsetDateTime.parse(value)
                .atZoneSameInstant(java.time.ZoneId.of("Asia/Shanghai"))
                .toLocalDateTime();
            LocalDateTime now = LocalDateTime.now();
            if (parsed.isAfter(now.plusMinutes(5)) || parsed.isBefore(now.minusDays(7))) {
                return now;
            }
            return parsed;
        } catch (Exception ignored) {
            try {
                long milliseconds = Long.parseLong(value);
                LocalDateTime parsed = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(milliseconds), ZoneOffset.ofHours(8)
                );
                return Duration.between(parsed, LocalDateTime.now()).abs().toDays() <= 7
                    ? parsed : LocalDateTime.now();
            } catch (Exception secondIgnored) {
                return LocalDateTime.now();
            }
        }
    }

    private Map<String, Object> sanitizeProperties(Map<String, ?> input) {
        if (input == null || input.isEmpty()) return Map.of();
        if (input.size() > MAX_PROPERTIES) throw BusinessException.badRequest("事件参数过多");
        Map<String, Object> result = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key == null || !key.matches("[a-z][a-z0-9_]{0,49}")) return;
            Object safe = sanitizeValue(value);
            if (safe != null) result.put(key, safe);
        });
        return result;
    }

    private Object sanitizeValue(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) return value;
        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>();
            collection.stream().limit(20).forEach(item -> {
                Object safe = sanitizeValue(item);
                if (safe != null) result.add(safe);
            });
            return result;
        }
        String text = value.toString().trim();
        text = text.replaceAll("(?<!\\d)1\\d{10}(?!\\d)", "***********");
        text = text.replaceAll("(?i)(?<![0-9X])\\d{6}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9X](?![0-9X])", "******************");
        text = sensitiveWords.mask(text);
        return text.substring(0, Math.min(text.length(), 200));
    }

    private String toJson(Map<String, Object> properties) {
        try {
            return objectMapper.writeValueAsString(properties == null ? Map.of() : properties);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String normalizePlatform(String value) {
        String platform = value == null ? "unknown" : value.trim().toLowerCase();
        return Set.of("android", "ios", "server", "unknown").contains(platform)
            ? platform : "unknown";
    }

    private String normalizeEnvironment() {
        String value = environment == null ? "prod" : environment.trim().toLowerCase();
        return Set.of("dev", "test", "prod").contains(value) ? value : "prod";
    }

    private String clean(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String text = value.trim();
        return text.substring(0, Math.min(text.length(), maxLength));
    }

    private String property(Map<String, ?> properties, String key, String fallback) {
        if (properties == null || properties.get(key) == null) return fallback;
        return clean(properties.get(key).toString(), 100);
    }

    public record ClientBatch(
        String anonymousId,
        String platform,
        String appVersion,
        List<ClientEvent> events
    ) {
    }

    public record ClientEvent(
        String eventId,
        String eventName,
        int eventVersion,
        String sessionId,
        String pageName,
        String sourcePage,
        String occurredAt,
        Map<String, Object> properties
    ) {
    }

    public record IngestResult(int attempted, int accepted, int duplicate) {
    }
}
