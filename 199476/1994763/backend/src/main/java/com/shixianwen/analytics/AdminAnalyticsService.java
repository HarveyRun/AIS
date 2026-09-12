package com.shixianwen.analytics;

import com.shixianwen.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {
    private static final Set<String> SECTIONS = Set.of(
        "overview", "funnel", "content", "supply", "answerers",
        "retention", "quality"
    );
    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public Map<String, Object> section(String section, Query query) {
        if (!SECTIONS.contains(section)) throw BusinessException.notFound("统计页面不存在");
        Filter filter = Filter.of(query);
        return switch (section) {
            case "overview" -> overview(filter);
            case "funnel" -> funnel(filter);
            case "content" -> content(filter);
            case "supply" -> supply(filter);
            case "answerers" -> answerers(filter);
            case "retention" -> retention(filter);
            case "quality" -> quality(filter);
            default -> throw BusinessException.notFound("统计页面不存在");
        };
    }

    @Transactional(readOnly = true)
    public PageResult raw(Query query, String eventName, int page, int size) {
        Filter filter = Filter.of(query);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        MapSqlParameterSource params = filter.params()
            .addValue("eventName", clean(eventName, 80))
            .addValue("limit", safeSize)
            .addValue("offset", safePage * safeSize);
        String where = where(filter) + " AND (:eventName='' OR event_name=:eventName)";
        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM analytics_events WHERE " + where,
            params,
            Long.class
        );
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT id,event_id AS eventId,event_name AS eventName,source_type AS sourceType," +
                "user_id AS userId,platform,app_version AS appVersion,page_name AS pageName," +
                "source_page AS sourcePage,test_account AS testAccount,properties," +
                "occurred_at AS occurredAt,received_at AS receivedAt " +
                "FROM analytics_events WHERE " + where +
                " ORDER BY occurred_at DESC,id DESC LIMIT :limit OFFSET :offset",
            params
        );
        return new PageResult(items, total == null ? 0 : total, safePage, safeSize);
    }

    private Map<String, Object> overview(Filter filter) {
        MapSqlParameterSource params = filter.params();
        String where = where(filter);
        Map<String, Object> events = one(
            "SELECT " +
                "COUNT(DISTINCT user_id) AS activeUsers," +
                "SUM(event_name='home_view') AS homeViews," +
                "SUM(event_name='profile_view') AS profileViews," +
                "SUM(event_name='inquiry_created') AS inquiriesCreated," +
                "SUM(event_name='inquiry_accepted') AS inquiriesAccepted," +
                "SUM(event_name='inquiry_settled') AS inquiriesSettled " +
                "FROM analytics_events WHERE " + where,
            params
        );
        Map<String, Object> money = one(
            "SELECT COALESCE(SUM(inquiry.amount),0) AS gmv," +
                "COALESCE(SUM(inquiry.service_fee_amount),0) AS serviceFee," +
                "COALESCE(SUM(inquiry.answerer_income_amount),0) AS answererIncome " +
                "FROM inquiries inquiry " +
                "JOIN users questioner ON questioner.id=inquiry.questioner_id " +
                "WHERE inquiry.status='COMPLETED' AND inquiry.ended_at>=:startTime " +
                "AND inquiry.ended_at<:endTime " +
                "AND (:platform='' OR LOWER(inquiry.client_platform)=:platform) " +
                "AND (:includeTest=TRUE OR questioner.account_type<>'TEST')",
            params
        );
        List<Map<String, Object>> trend = jdbc.queryForList(
            "SELECT DATE(occurred_at) AS day," +
                "SUM(event_name='home_view') AS homeViews," +
                "SUM(event_name='profile_view') AS profileViews," +
                "SUM(event_name='inquiry_created') AS inquiriesCreated," +
                "SUM(event_name='inquiry_settled') AS inquiriesSettled " +
                "FROM analytics_events WHERE " + where +
                " GROUP BY DATE(occurred_at) ORDER BY day",
            params
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cards", merge(events, money));
        result.put("trend", trend);
        result.put("updatedAt", LocalDateTime.now());
        return result;
    }

    private Map<String, Object> funnel(Filter filter) {
        String where = where(filter);
        MapSqlParameterSource params = filter.params();
        Map<String, Object> counts = one(
            "SELECT " +
                "SUM(event_name='home_view') AS homeView," +
                "SUM(event_name IN ('home_search_submit','profile_view')) AS discover," +
                "SUM(event_name='profile_view') AS profileView," +
                "SUM(event_name='inquiry_start') AS inquiryStart," +
                "SUM(event_name='inquiry_created') AS inquiryCreated," +
                "SUM(event_name='inquiry_accepted') AS inquiryAccepted," +
                "SUM(event_name='message_sent') AS messageSent," +
                "SUM(event_name='inquiry_settled') AS inquirySettled " +
                "FROM analytics_events WHERE " + where,
            params
        );
        List<Map<String, Object>> stages = List.of(
            stage("访问首页", counts.get("homeView")),
            stage("开始找人", counts.get("discover")),
            stage("查看个人信息", counts.get("profileView")),
            stage("点击询问", counts.get("inquiryStart")),
            stage("询问创建成功", counts.get("inquiryCreated")),
            stage("对方接受", counts.get("inquiryAccepted")),
            stage("产生交流", counts.get("messageSent")),
            stage("完成结算", counts.get("inquirySettled"))
        );
        return Map.of("stages", stages, "updatedAt", LocalDateTime.now());
    }

    private Map<String, Object> content(Filter filter) {
        return Map.of(
            "experiences", popularExperiences(filter),
            "banners", topProperty(filter, "banner_click", "banner_id", "banner_title"),
            "updatedAt", LocalDateTime.now()
        );
    }

    private List<Map<String, Object>> popularExperiences(Filter filter) {
        return jdbc.queryForList(
            "SELECT certification.id AS contentId,certification.title AS contentName," +
                "COUNT(DISTINCT inquiry.questioner_id) AS userCount,COUNT(inquiry.id) AS eventCount " +
                "FROM inquiries inquiry " +
                "JOIN certifications certification ON certification.id=inquiry.source_experience_certification_id " +
                "JOIN users questioner ON questioner.id=inquiry.questioner_id " +
                "WHERE inquiry.created_at>=:startTime AND inquiry.created_at<:endTime " +
                "AND (:platform='' OR LOWER(inquiry.client_platform)=:platform) " +
                "AND (:includeTest=TRUE OR questioner.account_type<>'TEST') " +
                "GROUP BY certification.id,certification.title " +
                "ORDER BY eventCount DESC,contentId DESC LIMIT 30",
            filter.params()
        );
    }

    private Map<String, Object> supply(Filter filter) {
        MapSqlParameterSource params = filter.params();
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT MIN(certification.id) AS experienceId,certification.title AS experienceName," +
                "COUNT(DISTINCT CASE WHEN user.accepting_inquiries=TRUE AND user.account_status='ACTIVE' " +
                "AND (:includeTest=TRUE OR user.account_type<>'TEST') THEN certification.user_id END) AS availablePeople," +
                "COUNT(inquiry.id) AS demandCount " +
                "FROM certifications certification " +
                "JOIN users user ON user.id=certification.user_id " +
                "LEFT JOIN inquiries inquiry ON inquiry.source_experience_certification_id=certification.id " +
                "AND inquiry.created_at>=:startTime AND inquiry.created_at<:endTime " +
                "AND (:platform='' OR LOWER(inquiry.client_platform)=:platform) " +
                "WHERE certification.category='EXPERIENCE' AND certification.status='APPROVED' " +
                "AND certification.enabled=TRUE AND certification.deleted_at IS NULL " +
                "GROUP BY certification.title ORDER BY demandCount DESC,availablePeople ASC,experienceId LIMIT 100",
            params
        );
        return Map.of("rows", rows, "updatedAt", LocalDateTime.now());
    }

    private Map<String, Object> answerers(Filter filter) {
        MapSqlParameterSource params = filter.params();
        Map<String, Object> current = one(
            "SELECT " +
                "SUM(answerer_status='APPROVED' AND account_status='ACTIVE') AS approvedAnswerers," +
                "SUM(answerer_status='APPROVED' AND account_status='ACTIVE' AND accepting_inquiries=TRUE) AS acceptingAnswerers," +
                "SUM(answerer_status='APPROVED' AND account_status='ACTIVE' AND accepting_inquiries=FALSE) AS pausedAnswerers " +
                "FROM users WHERE (:includeTest=TRUE OR account_type<>'TEST')",
            params
        );
        Map<String, Object> period = one(
            "SELECT " +
                "SUM(event_name='experience_submitted') AS experienceSubmitted," +
                "SUM(event_name='certification_approved') AS approved," +
                "SUM(event_name='certification_rejected') AS rejected " +
                "FROM analytics_events WHERE " + where(filter),
            params
        );
        return Map.of("cards", merge(current, period), "updatedAt", LocalDateTime.now());
    }

    private Map<String, Object> retention(Filter filter) {
        MapSqlParameterSource params = filter.params();
        Map<String, Object> result = one(
            "SELECT COUNT(*) AS inquiryUsers," +
                "SUM(summary.inquiryCount>=2) AS repeatUsers " +
                "FROM (SELECT inquiry.questioner_id,COUNT(*) AS inquiryCount " +
                "FROM inquiries inquiry JOIN users user ON user.id=inquiry.questioner_id " +
                "WHERE inquiry.created_at>=:startTime AND inquiry.created_at<:endTime " +
                "AND (:platform='' OR LOWER(inquiry.client_platform)=:platform) " +
                "AND (:includeTest=TRUE OR user.account_type<>'TEST') " +
                "GROUP BY inquiry.questioner_id) summary",
            params
        );
        Number users = result.get("inquiryUsers") instanceof Number value ? value : 0;
        Number repeats = result.get("repeatUsers") instanceof Number value ? value : 0;
        double repeatRate = users.longValue() == 0 ? 0 : repeats.doubleValue() * 100 / users.doubleValue();
        result.put("repeatRate", BigDecimal.valueOf(repeatRate).setScale(2, java.math.RoundingMode.HALF_UP));
        return Map.of("cards", result, "updatedAt", LocalDateTime.now());
    }

    private Map<String, Object> quality(Filter filter) {
        MapSqlParameterSource params = filter.params();
        Map<String, Object> events = one(
            "SELECT COUNT(*) AS storedEvents," +
                "SUM(source_type='CLIENT') AS clientEvents," +
                "SUM(source_type='SERVER') AS serverEvents," +
                "SUM(source_type='CLIENT' AND session_id IS NULL) AS missingSession," +
                "SUM(TIMESTAMPDIFF(MINUTE,occurred_at,received_at)>10) AS delayedEvents " +
                "FROM analytics_events WHERE " + where(filter),
            params
        );
        Map<String, Object> ingestion = one(
            "SELECT COALESCE(SUM(attempted_count),0) AS attempted," +
                "COALESCE(SUM(accepted_count),0) AS accepted," +
                "COALESCE(SUM(duplicate_count),0) AS duplicate " +
                "FROM analytics_ingestion_daily WHERE metric_date>=:startDate AND metric_date<=:endDate " +
                "AND environment=:environment AND (:platform='' OR platform=:platform)",
            params
        );
        return Map.of("cards", merge(events, ingestion), "updatedAt", LocalDateTime.now());
    }

    private List<Map<String, Object>> topProperty(
        Filter filter,
        String eventName,
        String idProperty,
        String nameProperty
    ) {
        MapSqlParameterSource params = filter.params().addValue("eventName", eventName);
        String idPath = "$." + idProperty;
        String namePath = "$." + nameProperty;
        return jdbc.queryForList(
            "SELECT JSON_UNQUOTE(JSON_EXTRACT(properties,'" + idPath + "')) AS contentId," +
                "COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(properties,'" + namePath + "')),'null'),'未命名') AS contentName," +
                "COUNT(*) AS eventCount,COUNT(DISTINCT user_id) AS userCount " +
                "FROM analytics_events WHERE " + where(filter) +
                " AND event_name=:eventName AND JSON_EXTRACT(properties,'" + idPath + "') IS NOT NULL " +
                "GROUP BY contentId,contentName ORDER BY eventCount DESC LIMIT 30",
            params
        );
    }

    private String where(Filter filter) {
        return "occurred_at>=:startTime AND occurred_at<:endTime " +
            "AND environment=:environment " +
            "AND (:includeTest=TRUE OR test_account=FALSE) " +
            "AND (:platform='' OR platform=:platform)";
    }

    private Map<String, Object> one(String sql, MapSqlParameterSource params) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        return rows.isEmpty() ? new LinkedHashMap<>() : new LinkedHashMap<>(rows.get(0));
    }

    private Map<String, Object> merge(Map<String, Object> first, Map<String, Object> second) {
        Map<String, Object> result = new LinkedHashMap<>(first);
        result.putAll(second);
        result.replaceAll((key, value) -> value == null ? 0 : value);
        return result;
    }

    private Map<String, Object> stage(String label, Object count) {
        return Map.of("label", label, "count", count == null ? 0 : count);
    }

    private String clean(String value, int max) {
        if (value == null) return "";
        String text = value.trim();
        return text.substring(0, Math.min(text.length(), max));
    }

    public record Query(
        LocalDate startDate,
        LocalDate endDate,
        String platform,
        String environment,
        boolean includeTest
    ) {
    }

    private record Filter(
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String platform,
        String environment,
        boolean includeTest
    ) {
        static Filter of(Query query) {
            LocalDate today = LocalDate.now();
            LocalDate start = query == null || query.startDate() == null ? today.minusDays(6) : query.startDate();
            LocalDate end = query == null || query.endDate() == null ? today : query.endDate();
            if (end.isBefore(start)) throw BusinessException.badRequest("结束日期不能早于开始日期");
            if (start.plusDays(366).isBefore(end)) throw BusinessException.badRequest("单次最多查询一年");
            String platform = query == null || query.platform() == null ? "" : query.platform().trim().toLowerCase();
            if (!Set.of("", "android", "ios", "server", "unknown").contains(platform)) {
                throw BusinessException.badRequest("客户端类型不正确");
            }
            String environment = query == null || query.environment() == null
                ? "prod" : query.environment().trim().toLowerCase();
            if (!Set.of("dev", "test", "prod").contains(environment)) {
                throw BusinessException.badRequest("运行环境不正确");
            }
            return new Filter(
                start, end, start.atStartOfDay(), end.plusDays(1).atStartOfDay(),
                platform, environment, query != null && query.includeTest()
            );
        }

        MapSqlParameterSource params() {
            return new MapSqlParameterSource()
                .addValue("startDate", startDate)
                .addValue("endDate", endDate)
                .addValue("startTime", startTime)
                .addValue("endTime", endTime)
                .addValue("platform", platform)
                .addValue("environment", environment)
                .addValue("includeTest", includeTest);
        }
    }

    public record PageResult(List<Map<String, Object>> items, long total, int page, int size) {
    }
}
