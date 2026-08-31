package com.shixianwen.inquiry;

import com.shixianwen.common.BusinessException;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.PermanentBanPayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ViolationService {
    private static final int[] THRESHOLDS = {1, 3, 5, 10, 30, 50, 70, 100, 120, 200};

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final NotificationService notifications;
    private final RealtimePublisher realtime;
    private final PermanentBanPayoutService permanentBanPayouts;

    @Transactional
    public ViolationResult applyForInquiry(
        Long userId,
        Long inquiryId,
        String sourceType,
        Long sourceId,
        int level,
        String fact,
        String ruleBasis
    ) {
        validateLevel(level);
        User user = users.findById(userId)
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        jdbc.update(
            "INSERT IGNORE INTO user_violation_records(" +
                "user_id,inquiry_id,source_type,source_id,violation_level,fact_description,rule_basis" +
                ") VALUES(?,?,?,?,?,?,?)",
            userId, inquiryId, sourceType, sourceId, level, clean(fact, 500), clean(ruleBasis, 500)
        );
        Map<String, Object> record = jdbc.queryForMap(
            "SELECT id,violation_level AS violationLevel,active FROM user_violation_records " +
                "WHERE source_type=? AND source_id=? AND user_id=? FOR UPDATE",
            sourceType, sourceId, userId
        );
        long recordId = ((Number) record.get("id")).longValue();
        List<Map<String, Object>> summaries = jdbc.queryForList(
            "SELECT violation_level AS violationLevel,source_record_id AS sourceRecordId " +
                "FROM user_inquiry_violation_summaries WHERE user_id=? AND inquiry_id=? FOR UPDATE",
            userId, inquiryId
        );

        Integer replacedLevel = null;
        if (!summaries.isEmpty()) {
            int currentLevel = ((Number) summaries.get(0).get("violationLevel")).intValue();
            if (currentLevel <= level) {
                jdbc.update("UPDATE user_violation_records SET active=FALSE WHERE id=?", recordId);
                return currentResult(userId, currentLevel, false);
            }
            replacedLevel = currentLevel;
            decrementCounter(userId, currentLevel);
            jdbc.update(
                "UPDATE user_violation_records SET active=FALSE WHERE id=?",
                ((Number) summaries.get(0).get("sourceRecordId")).longValue()
            );
        }

        Counter counter = incrementCounter(userId, level);
        jdbc.update("UPDATE user_violation_records SET active=TRUE WHERE id=?", recordId);
        jdbc.update(
            "INSERT INTO user_inquiry_violation_summaries(user_id,inquiry_id,violation_level,source_record_id) " +
                "VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE violation_level=VALUES(violation_level)," +
                "source_record_id=VALUES(source_record_id)",
            userId, inquiryId, level, recordId
        );

        BanResult ban = applyAutomaticBan(user, level, counter.usedCount(), counter.threshold());
        refreshRiskWatch(userId);
        if (ban.banned()) {
            notifications.send(
                user,
                "账号处罚通知",
                ban.banUntil() == null ? "账号因违反平台规则已被永久封禁" : "账号因违反平台规则已被限期封禁",
                "/profile"
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("reason", "违反平台规则");
            payload.put("banUntil", ban.banUntil());
            payload.put("permanent", ban.banUntil() == null);
            realtime.afterCommit(userId, "ACCOUNT_PENALTY", payload);
        } else {
            String text = "事实：" + clean(fact, 500) + "；规则依据：" + clean(ruleBasis, 500) +
                "；本次记为" + level + "级违规，当前已记录" + counter.usedCount() + "/" +
                counter.threshold() + "次。如有异议，可通过投诉与反馈提交申诉。";
            notifications.send(user, "违规处理通知", text, "/profile/feedback");
        }
        return new ViolationResult(
            level,
            counter.usedCount(),
            counter.threshold(),
            replacedLevel,
            ban.banned(),
            ban.banUntil(),
            true
        );
    }

    @Transactional
    public ViolationResult applyForSource(
        Long userId,
        String sourceType,
        Long sourceId,
        int level,
        String fact,
        String ruleBasis
    ) {
        validateLevel(level);
        String normalizedSourceType = clean(sourceType, 40).toUpperCase();
        String normalizedFact = clean(fact, 500);
        String normalizedRuleBasis = clean(ruleBasis, 500);
        User user = users.findById(userId)
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        int inserted = jdbc.update(
            "INSERT IGNORE INTO user_violation_records(" +
                "user_id,inquiry_id,source_type,source_id,violation_level,fact_description,rule_basis" +
                ") VALUES(?,NULL,?,?,?,?,?)",
            userId,
            normalizedSourceType,
            sourceId,
            level,
            normalizedFact,
            normalizedRuleBasis
        );
        if (inserted == 0) {
            Integer existingLevel = jdbc.queryForObject(
                "SELECT violation_level FROM user_violation_records " +
                    "WHERE source_type=? AND source_id=? AND user_id=?",
                Integer.class,
                normalizedSourceType,
                sourceId,
                userId
            );
            return currentResult(userId, existingLevel == null ? level : existingLevel, false);
        }

        Counter counter = incrementCounter(userId, level);
        BanResult ban = applyAutomaticBan(user, level, counter.usedCount(), counter.threshold());
        refreshRiskWatch(userId);
        if (ban.banned()) {
            notifications.send(
                user,
                "账号处罚通知",
                ban.banUntil() == null ? "账号因违反平台规则已被永久封禁" : "账号因违反平台规则已被限期封禁",
                "/profile"
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("reason", "违反平台规则");
            payload.put("banUntil", ban.banUntil());
            payload.put("permanent", ban.banUntil() == null);
            realtime.afterCommit(userId, "ACCOUNT_PENALTY", payload);
        } else {
            notifications.send(
                user,
                "违规处理通知",
                "事实：" + normalizedFact + "；规则依据：" + normalizedRuleBasis +
                    "；本次记为" + level + "级违规，当前已记录" + counter.usedCount() + "/" +
                    counter.threshold() + "次。如有异议，可通过投诉与反馈提交申诉。",
                "/profile/feedback"
            );
        }
        return new ViolationResult(
            level,
            counter.usedCount(),
            counter.threshold(),
            null,
            ban.banned(),
            ban.banUntil(),
            true
        );
    }

    private Counter incrementCounter(Long userId, int level) {
        int threshold = THRESHOLDS[level];
        jdbc.update(
            "INSERT INTO user_violation_counters(user_id,violation_level,used_count,threshold_count) " +
                "VALUES(?,?,1,?) ON DUPLICATE KEY UPDATE used_count=used_count+1,threshold_count=VALUES(threshold_count)",
            userId, level, threshold
        );
        Map<String, Object> row = jdbc.queryForMap(
            "SELECT used_count AS usedCount,threshold_count AS thresholdCount " +
                "FROM user_violation_counters WHERE user_id=? AND violation_level=? FOR UPDATE",
            userId, level
        );
        return new Counter(
            ((Number) row.get("usedCount")).intValue(),
            ((Number) row.get("thresholdCount")).intValue()
        );
    }

    private void decrementCounter(Long userId, int level) {
        jdbc.update(
            "UPDATE user_violation_counters SET used_count=GREATEST(used_count-1,0) " +
                "WHERE user_id=? AND violation_level=?",
            userId, level
        );
    }

    private BanResult applyAutomaticBan(User user, int level, int usedCount, int threshold) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime proposedUntil = null;
        boolean permanent = level == 0 || usedCount >= threshold;
        if (!permanent) {
            proposedUntil = switch (level) {
                case 1 -> now.plusDays(15);
                case 2 -> now.plusDays(7);
                case 3 -> now.plusDays(3);
                default -> null;
            };
            if (proposedUntil == null) return new BanResult(false, user.getBanUntil());
        }

        if ("SUSPENDED".equals(user.getAccountStatus()) && user.getBanUntil() == null) {
            permanentBanPayouts.captureForPermanentBan(user.getId());
            return new BanResult(true, null);
        }
        LocalDateTime finalUntil = permanent
            ? null
            : later(user.getBanUntil(), proposedUntil);
        user.setAccountStatus("SUSPENDED");
        user.setBanReason("违反平台规则");
        user.setBannedAt(now);
        user.setBanUntil(finalUntil);
        user.setBannedByAdmin(null);
        users.saveAndFlush(user);
        if (permanent) {
            permanentBanPayouts.captureForPermanentBan(user.getId());
        }
        return new BanResult(true, finalUntil);
    }

    private void refreshRiskWatch(Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT used_count AS usedCount,threshold_count AS thresholdCount " +
                "FROM user_violation_counters WHERE user_id=? AND used_count>0",
            userId
        );
        boolean reached = rows.stream().anyMatch(row ->
            ((Number) row.get("usedCount")).intValue() >= ((Number) row.get("thresholdCount")).intValue()
        );
        double score = rows.stream().mapToDouble(row ->
            ((Number) row.get("usedCount")).doubleValue() /
                ((Number) row.get("thresholdCount")).doubleValue()
        ).sum();
        if (!reached && score >= 1D) {
            jdbc.update(
                "INSERT INTO user_risk_watchlist(user_id,risk_score,reason,status) VALUES(?,?,?,'WATCHING') " +
                    "ON DUPLICATE KEY UPDATE risk_score=VALUES(risk_score),reason=VALUES(reason)," +
                    "status=IF(status='DISMISSED','WATCHING',status),updated_at=NOW(6)",
                userId, score, "多个违规等级累计比例已达到综合风险关注标准"
            );
        } else {
            jdbc.update(
                "UPDATE user_risk_watchlist SET risk_score=?,updated_at=NOW(6) WHERE user_id=?",
                score, userId
            );
        }
    }

    private ViolationResult currentResult(Long userId, int level, boolean applied) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT used_count AS usedCount,threshold_count AS thresholdCount " +
                "FROM user_violation_counters WHERE user_id=? AND violation_level=?",
            userId, level
        );
        int used = rows.isEmpty() ? 0 : ((Number) rows.get(0).get("usedCount")).intValue();
        int threshold = rows.isEmpty() ? THRESHOLDS[level] :
            ((Number) rows.get(0).get("thresholdCount")).intValue();
        return new ViolationResult(level, used, threshold, null, false, null, applied);
    }

    @Transactional(readOnly = true)
    public List<ViolationCounterView> currentCounters(Long userId) {
        Map<Integer, Integer> usedCounts = new LinkedHashMap<>();
        jdbc.queryForList(
            "SELECT violation_level AS violationLevel,used_count AS usedCount " +
                "FROM user_violation_counters WHERE user_id=?",
            userId
        ).forEach(row -> usedCounts.put(
            ((Number) row.get("violationLevel")).intValue(),
            ((Number) row.get("usedCount")).intValue()
        ));
        return IntStream.range(0, THRESHOLDS.length)
            .mapToObj(level -> {
                int used = usedCounts.getOrDefault(level, 0);
                int threshold = THRESHOLDS[level];
                return new ViolationCounterView(
                    level,
                    used,
                    threshold,
                    Math.max(threshold - used, 0)
                );
            })
            .toList();
    }

    private LocalDateTime later(LocalDateTime first, LocalDateTime second) {
        if (first == null || !first.isAfter(LocalDateTime.now())) return second;
        return first.isAfter(second) ? first : second;
    }

    private void validateLevel(int level) {
        if (level < 0 || level > 9) throw BusinessException.badRequest("违规等级必须为0至9级");
    }

    private String clean(String value, int max) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty()) throw BusinessException.badRequest("违规事实和规则依据不能为空");
        return result.substring(0, Math.min(result.length(), max));
    }

    private record Counter(int usedCount, int threshold) {
    }

    private record BanResult(boolean banned, LocalDateTime banUntil) {
    }

    public record ViolationResult(
        int level,
        int usedCount,
        int threshold,
        Integer replacedLevel,
        boolean banned,
        LocalDateTime banUntil,
        boolean applied
    ) {
    }

    public record ViolationCounterView(
        int level,
        int usedCount,
        int thresholdCount,
        int remainingCount
    ) {
    }
}
