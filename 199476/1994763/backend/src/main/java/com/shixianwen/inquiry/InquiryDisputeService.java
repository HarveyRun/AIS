package com.shixianwen.inquiry;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.common.BusinessException;
import com.shixianwen.content.SensitiveContentCipher;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class InquiryDisputeService {
    private static final Map<String, Integer> REPORT_LEVELS = Map.of(
        "LOW_RELEVANCE", 8,
        "PORN_GAMBLING_DRUGS", 5,
        "NATIONAL_SECURITY", 3
    );
    private static final Set<String> REPORT_DECISIONS = Set.of("PASSED", "REJECTED", "IGNORED");

    private final JdbcTemplate jdbc;
    private final InquiryRepository inquiries;
    private final InquiryMessageRepository messages;
    private final UserRepository users;
    private final SensitiveContentCipher cipher;
    private final InquiryService inquiryService;
    private final ViolationService violations;
    private final NotificationService notifications;

    @Transactional(readOnly = true)
    public MessageReportStatus reportStatus(Long userId, Long inquiryId, Long messageId) {
        Inquiry inquiry = participant(userId, inquiryId);
        InquiryMessage message = messages.findById(messageId)
            .orElseThrow(() -> BusinessException.notFound("消息不存在"));
        if (!message.getInquiry().getId().equals(inquiry.getId())) {
            throw BusinessException.badRequest("消息不属于当前询问");
        }
        if (message.getSender() == null || !Set.of("TEXT", "IMAGE").contains(message.getMessageType())) {
            throw BusinessException.badRequest("该消息不能举报");
        }
        if (message.getSender().getId().equals(userId)) {
            throw BusinessException.badRequest("不能举报自己发送的消息");
        }
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM inquiry_message_reports WHERE reporter_id=? AND message_id=?",
            Integer.class, userId, messageId
        );
        return new MessageReportStatus(count != null && count > 0);
    }

    @Transactional
    public MessageReportView report(Long userId, Long inquiryId, Long messageId, String reportType) {
        Inquiry inquiry = participant(userId, inquiryId);
        InquiryMessage message = messages.findById(messageId)
            .orElseThrow(() -> BusinessException.notFound("消息不存在"));
        if (!message.getInquiry().getId().equals(inquiryId)) {
            throw BusinessException.badRequest("消息不属于当前询问");
        }
        if (message.getSender() == null || !Set.of("TEXT", "IMAGE").contains(message.getMessageType())) {
            throw BusinessException.badRequest("该消息不能举报");
        }
        if (message.getSender().getId().equals(userId)) {
            throw BusinessException.badRequest("不能举报自己发送的消息");
        }
        String type = reportType == null ? "" : reportType.trim().toUpperCase(Locale.ROOT);
        Integer level = REPORT_LEVELS.get(type);
        if (level == null) throw BusinessException.badRequest("请选择举报类型");
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        Integer dailyCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM inquiry_message_reports WHERE reporter_id=? AND created_at>=?",
            Integer.class, userId, startOfDay
        );
        if (dailyCount != null && dailyCount >= 20) {
            throw BusinessException.tooManyRequests("今日举报数量已达上限");
        }
        Integer duplicated = jdbc.queryForObject(
            "SELECT COUNT(*) FROM inquiry_message_reports WHERE reporter_id=? AND message_id=?",
            Integer.class, userId, messageId
        );
        if (duplicated != null && duplicated > 0) throw BusinessException.badRequest("你已经举报过这条消息");

        jdbc.update(
            "INSERT IGNORE INTO inquiry_message_report_cases(inquiry_id) VALUES(?)",
            inquiryId
        );
        Map<String, Object> reportCase = jdbc.queryForMap(
            "SELECT id,status FROM inquiry_message_report_cases WHERE inquiry_id=? FOR UPDATE",
            inquiryId
        );
        if ("RESOLVED".equals(reportCase.get("status"))) {
            throw BusinessException.badRequest("该订单的消息举报已经处理完成");
        }
        long caseId = ((Number) reportCase.get("id")).longValue();
        String snapshot = "IMAGE".equals(message.getMessageType()) ? "[图片]" : message.getContent();
        String rawSnapshot = "IMAGE".equals(message.getMessageType())
            ? cipher.encrypt(
                "attachmentKey=" + safe(message.getAttachmentKey()) +
                    ";name=" + safe(message.getAttachmentName()) +
                    ";size=" + (message.getAttachmentSize() == null ? 0 : message.getAttachmentSize())
            )
            : message.getRawContentEncrypted();
        jdbc.update(
            "INSERT INTO inquiry_message_reports(" +
                "case_id,inquiry_id,message_id,reporter_id,reported_user_id,report_type,violation_level," +
                "masked_content_snapshot,raw_content_snapshot_encrypted,question_snapshot" +
                ") VALUES(?,?,?,?,?,?,?,?,?,?)",
            caseId, inquiryId, messageId, userId, message.getSender().getId(), type, level,
            snapshot, rawSnapshot, inquiry.getQuestion()
        );
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        recordEvent(inquiryId, "MESSAGE_REPORTED", "USER", userId, "messageId=" + messageId + ";type=" + type);
        return new MessageReportView(id, inquiryId, messageId, type, level, "PENDING", LocalDateTime.now());
    }

    public PageResult endDisputes(String keyword, String status, int page, int size) {
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        String cleanStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String like = "%" + cleanKeyword + "%";
        String where = " WHERE (?='' OR d.status=?) AND (?='' OR q.uid LIKE ? OR q.phone LIKE ? OR a.uid LIKE ? OR a.phone LIKE ?) ";
        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM inquiry_end_disputes d JOIN inquiries i ON i.id=d.inquiry_id " +
                "JOIN users q ON q.id=i.questioner_id JOIN users a ON a.id=i.answerer_id" + where,
            Long.class, cleanStatus, cleanStatus, cleanKeyword, like, like, like, like
        );
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT d.id,d.inquiry_id AS inquiryId,d.trigger_type AS triggerType,d.status,d.decision," +
                "d.decision_reason AS decisionReason,d.created_at AS createdAt,i.question,i.amount," +
                "i.settleable_amount AS settleableAmount,i.timeout_refunded_amount AS timeoutRefundedAmount," +
                "q.id AS questionerId,q.uid AS questionerUid,q.phone AS questionerPhone," +
                "a.id AS answererId,a.uid AS answererUid,a.phone AS answererPhone " +
                "FROM inquiry_end_disputes d JOIN inquiries i ON i.id=d.inquiry_id " +
                "JOIN users q ON q.id=i.questioner_id JOIN users a ON a.id=i.answerer_id" + where +
                "ORDER BY (d.status='PENDING') DESC,d.id DESC LIMIT ? OFFSET ?",
            cleanStatus, cleanStatus, cleanKeyword, like, like, like, like, size, page * size
        );
        return new PageResult(items, total == null ? 0 : total, page, size);
    }

    @Transactional
    public void resolveEndDispute(
        AdminUser admin,
        Long disputeId,
        String decision,
        String reason,
        String ip
    ) {
        String normalized = decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("QUESTIONER_VIOLATION", "ANSWERER_VIOLATION").contains(normalized)) {
            throw BusinessException.badRequest("请选择纠纷处理结果");
        }
        Map<String, Object> row = singleForUpdate(
            "SELECT d.id,d.status,d.inquiry_id AS inquiryId,i.questioner_id AS questionerId," +
                "i.answerer_id AS answererId FROM inquiry_end_disputes d " +
                "JOIN inquiries i ON i.id=d.inquiry_id WHERE d.id=? FOR UPDATE",
            disputeId, "结束纠纷不存在"
        );
        if (!"PENDING".equals(row.get("status"))) throw BusinessException.badRequest("结束纠纷已经处理");
        long inquiryId = number(row, "inquiryId");
        long questionerId = number(row, "questionerId");
        long answererId = number(row, "answererId");
        String cleanReason = required(reason, 500, "请填写处理依据");
        boolean questionerViolation = "QUESTIONER_VIOLATION".equals(normalized);
        inquiryService.resolveEndDisputeFunds(inquiryId, questionerViolation);
        jdbc.update(
            "UPDATE inquiry_end_disputes SET status='RESOLVED',decision=?,decision_reason=?," +
                "reviewed_by_admin_id=?,reviewed_at=NOW(6) WHERE id=?",
            normalized, cleanReason, admin.getId(), disputeId
        );
        if (questionerViolation) {
            violations.applyForInquiry(
                questionerId, inquiryId, "END_DISPUTE", disputeId, 5,
                "未妥善处理本次询问的结束申请，经平台判定由提问者承担责任",
                "《询问交易规则》结束申请与纠纷处理条款"
            );
        } else {
            violations.applyForInquiry(
                answererId, inquiryId, "END_DISPUTE", disputeId, 4,
                "本次结束申请经平台判定由回答者承担责任",
                "《询问交易规则》结束申请与纠纷处理条款"
            );
        }
        recordEvent(inquiryId, "END_DISPUTE_RESOLVED", "ADMIN", admin.getId(), normalized + ";" + cleanReason);
        audit(admin, "RESOLVE_END_DISPUTE", "INQUIRY_END_DISPUTE", disputeId, normalized + " | " + cleanReason, ip);
    }

    public PageResult reportCases(String keyword, String status, int page, int size) {
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        String cleanStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String like = "%" + cleanKeyword + "%";
        String where = " WHERE (?='' OR c.status=?) AND (?='' OR q.uid LIKE ? OR q.phone LIKE ? OR a.uid LIKE ? OR a.phone LIKE ?) ";
        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM inquiry_message_report_cases c JOIN inquiries i ON i.id=c.inquiry_id " +
                "JOIN users q ON q.id=i.questioner_id JOIN users a ON a.id=i.answerer_id" + where,
            Long.class, cleanStatus, cleanStatus, cleanKeyword, like, like, like, like
        );
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT c.id,c.inquiry_id AS inquiryId,c.status,c.created_at AS createdAt,i.status AS inquiryStatus," +
                "i.question,q.uid AS questionerUid,q.phone AS questionerPhone,a.uid AS answererUid," +
                "a.phone AS answererPhone,COUNT(r.id) AS reportCount " +
                "FROM inquiry_message_report_cases c JOIN inquiries i ON i.id=c.inquiry_id " +
                "JOIN users q ON q.id=i.questioner_id JOIN users a ON a.id=i.answerer_id " +
                "LEFT JOIN inquiry_message_reports r ON r.case_id=c.id" + where +
                "GROUP BY c.id,c.inquiry_id,c.status,c.created_at,i.status,i.question,q.uid,q.phone,a.uid,a.phone " +
                "ORDER BY (c.status='PENDING') DESC,c.id DESC LIMIT ? OFFSET ?",
            cleanStatus, cleanStatus, cleanKeyword, like, like, like, like, size, page * size
        );
        return new PageResult(items, total == null ? 0 : total, page, size);
    }

    public Map<String, Object> reportCaseDetail(Long caseId) {
        Map<String, Object> header = single(
            "SELECT c.id,c.inquiry_id AS inquiryId,c.status,i.status AS inquiryStatus,i.question," +
                "i.amount,i.settleable_amount AS settleableAmount,q.uid AS questionerUid,a.uid AS answererUid " +
                "FROM inquiry_message_report_cases c JOIN inquiries i ON i.id=c.inquiry_id " +
                "JOIN users q ON q.id=i.questioner_id JOIN users a ON a.id=i.answerer_id WHERE c.id=?",
            caseId, "消息举报不存在"
        );
        List<Map<String, Object>> reports = jdbc.queryForList(
            "SELECT r.id,r.message_id AS messageId,r.reporter_id AS reporterId,ru.uid AS reporterUid," +
                "r.reported_user_id AS reportedUserId,tu.uid AS reportedUid,r.report_type AS reportType," +
                "r.violation_level AS violationLevel,r.status,r.decision_reason AS decisionReason," +
                "r.masked_content_snapshot AS content,r.created_at AS createdAt " +
                "FROM inquiry_message_reports r JOIN users ru ON ru.id=r.reporter_id " +
                "JOIN users tu ON tu.id=r.reported_user_id WHERE r.case_id=? ORDER BY r.id",
            caseId
        );
        Map<String, Object> result = new LinkedHashMap<>(header);
        result.put("reports", reports);
        return result;
    }

    @Transactional
    public void resolveReportCase(
        AdminUser admin,
        Long caseId,
        List<ReportDecision> decisions,
        String ip
    ) {
        Map<String, Object> header = singleForUpdate(
            "SELECT c.id,c.status,c.inquiry_id AS inquiryId,i.status AS inquiryStatus " +
                "FROM inquiry_message_report_cases c JOIN inquiries i ON i.id=c.inquiry_id " +
                "WHERE c.id=? FOR UPDATE",
            caseId, "消息举报不存在"
        );
        if (!"PENDING".equals(header.get("status"))) throw BusinessException.badRequest("消息举报已经处理");
        if (Set.of("PENDING", "ACTIVE", "AWAITING_CONFIRMATION").contains(header.get("inquiryStatus"))) {
            throw BusinessException.badRequest("订单结束后才能处理消息举报");
        }
        List<Map<String, Object>> reports = jdbc.queryForList(
            "SELECT id,reporter_id AS reporterId,reported_user_id AS reportedUserId," +
                "report_type AS reportType,violation_level AS violationLevel FROM inquiry_message_reports " +
                "WHERE case_id=? ORDER BY id FOR UPDATE",
            caseId
        );
        Map<Long, ReportDecision> decisionMap = new LinkedHashMap<>();
        if (decisions != null) decisions.forEach(item -> decisionMap.put(item.reportId(), item));
        if (decisionMap.size() != reports.size()) throw BusinessException.badRequest("请处理全部举报明细");
        long inquiryId = number(header, "inquiryId");
        Map<Long, PunishmentCandidate> punishmentByUser = new LinkedHashMap<>();
        List<IgnoredNotice> ignoredNotices = new ArrayList<>();
        for (Map<String, Object> report : reports) {
            long reportId = number(report, "id");
            ReportDecision decision = decisionMap.get(reportId);
            if (decision == null) throw BusinessException.badRequest("举报处理明细不完整");
            String status = decision.status() == null ? "" : decision.status().trim().toUpperCase(Locale.ROOT);
            if (!REPORT_DECISIONS.contains(status)) throw BusinessException.badRequest("举报处理状态不正确");
            String reason = required(decision.reason(), 500, "请填写每条举报的处理依据");
            jdbc.update(
                "UPDATE inquiry_message_reports SET status=?,decision_reason=?,reviewed_at=NOW(6) WHERE id=?",
                status, reason, reportId
            );
            if ("PASSED".equals(status)) {
                mergePunishment(
                    punishmentByUser,
                    new PunishmentCandidate(
                        number(report, "reportedUserId"),
                        ((Number) report.get("violationLevel")).intValue(),
                        "订单中的消息举报经平台核实通过，类型：" +
                            reportTypeText(String.valueOf(report.get("reportType"))),
                        "《聊天内容举报与违规处理规则》"
                    )
                );
            } else if ("REJECTED".equals(status)) {
                mergePunishment(
                    punishmentByUser,
                    new PunishmentCandidate(
                        number(report, "reporterId"),
                        3,
                        "订单中的消息举报经平台核实后被驳回",
                        "《聊天内容举报与违规处理规则》举报责任条款"
                    )
                );
            } else {
                ignoredNotices.add(new IgnoredNotice(number(report, "reporterId"), reason));
            }
            recordEvent(inquiryId, "MESSAGE_REPORT_RESOLVED", "ADMIN", admin.getId(),
                "reportId=" + reportId + ";status=" + status + ";reason=" + reason);
        }
        punishmentByUser.values().forEach(candidate -> violations.applyForInquiry(
            candidate.userId(), inquiryId, "MESSAGE_REPORT_CASE", caseId, candidate.level(),
            candidate.fact(), candidate.ruleBasis()
        ));
        ignoredNotices.forEach(notice -> notifications.send(
            user(notice.userId()),
            "举报处理结果",
            "本条举报已作忽略处理：" + notice.reason(),
            "/inquiries/" + inquiryId
        ));
        jdbc.update(
            "UPDATE inquiry_message_report_cases SET status='RESOLVED',reviewed_by_admin_id=?,reviewed_at=NOW(6) WHERE id=?",
            admin.getId(), caseId
        );
        audit(admin, "RESOLVE_MESSAGE_REPORT_CASE", "MESSAGE_REPORT_CASE", caseId, "已处理" + reports.size() + "条举报", ip);
    }

    public Map<String, Object> originalEvidence(
        AdminUser admin,
        Long reportId,
        String ip
    ) {
        Map<String, Object> row = single(
            "SELECT r.id,r.raw_content_snapshot_encrypted AS rawContent,i.question_raw_encrypted AS rawQuestion " +
                "FROM inquiry_message_reports r JOIN inquiries i ON i.id=r.inquiry_id WHERE r.id=?",
            reportId, "举报记录不存在"
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", reportId);
        result.put("messageOriginal", cipher.decrypt((String) row.get("rawContent")));
        result.put("questionOriginal", cipher.decrypt((String) row.get("rawQuestion")));
        audit(admin, "VIEW_SENSITIVE_ORIGINAL", "MESSAGE_REPORT", reportId, "查看举报原始证据", ip);
        return result;
    }

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
        audit(admin, "DISMISS_RISK_WATCH", "USER", userId, cleanReason, ip);
    }

    private Inquiry participant(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiries.findById(inquiryId)
            .orElseThrow(() -> BusinessException.notFound("询问不存在"));
        if (!inquiry.getQuestioner().getId().equals(userId)
            && !inquiry.getAnswerer().getId().equals(userId)) {
            throw BusinessException.forbidden("无权操作该询问");
        }
        return inquiry;
    }

    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }

    private Map<String, Object> single(String sql, Long id, String message) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, id);
        if (rows.isEmpty()) throw BusinessException.notFound(message);
        return rows.get(0);
    }

    private Map<String, Object> singleForUpdate(String sql, Long id, String message) {
        return single(sql, id, message);
    }

    private long number(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
    }

    private String required(String value, int max, String message) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty()) throw BusinessException.badRequest(message);
        return result.substring(0, Math.min(result.length(), max));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void mergePunishment(
        Map<Long, PunishmentCandidate> target,
        PunishmentCandidate candidate
    ) {
        PunishmentCandidate current = target.get(candidate.userId());
        if (current == null || candidate.level() < current.level()) {
            target.put(candidate.userId(), candidate);
        }
    }

    private String reportTypeText(String type) {
        return switch (type) {
            case "LOW_RELEVANCE" -> "和原始提问相关性极低";
            case "PORN_GAMBLING_DRUGS" -> "黄赌毒内容";
            case "NATIONAL_SECURITY" -> "危害国家安全、破坏社会稳定的言论";
            default -> type;
        };
    }

    private void recordEvent(Long inquiryId, String eventType, String actorType, Long actorId, String detail) {
        String hash = sha256(inquiryId + "|" + eventType + "|" + actorType + "|" + actorId + "|" + detail);
        jdbc.update(
            "INSERT INTO inquiry_dispute_events(inquiry_id,event_type,actor_type,actor_id,detail,evidence_hash) " +
                "VALUES(?,?,?,?,?,?)",
            inquiryId, eventType, actorType, actorId, required(detail, 1000, "证据内容不能为空"), hash
        );
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void audit(AdminUser admin, String action, String targetType, Long targetId, String detail, String ip) {
        jdbc.update(
            "INSERT INTO admin_audit_logs(admin_user_id,action,target_type,target_id,detail,ip_address) " +
                "VALUES(?,?,?,?,?,?)",
            admin.getId(), action, targetType, String.valueOf(targetId), detail, ip
        );
    }

    public record MessageReportView(
        Long id,
        Long inquiryId,
        Long messageId,
        String reportType,
        int violationLevel,
        String status,
        LocalDateTime createdAt
    ) {
    }

    public record MessageReportStatus(boolean reported) {
    }

    public record ReportDecision(Long reportId, String status, String reason) {
    }

    private record PunishmentCandidate(Long userId, int level, String fact, String ruleBasis) {
    }

    private record IgnoredNotice(Long userId, String reason) {
    }

    public record PageResult(List<Map<String, Object>> items, long total, int page, int size) {
    }
}
