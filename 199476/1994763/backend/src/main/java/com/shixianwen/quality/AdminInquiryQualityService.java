package com.shixianwen.quality;

import com.shixianwen.admin.AdminAuditLog;
import com.shixianwen.admin.AdminAuditLogRepository;
import com.shixianwen.admin.AdminManagementService;
import com.shixianwen.admin.AdminAuthorizationService;
import com.shixianwen.admin.AdminUser;
import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.common.BusinessException;
import com.shixianwen.inquiry.Inquiry;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminInquiryQualityService {
    private final JdbcTemplate jdbc;
    private final InquiryQualityReviewRepository reviews;
    private final WalletService wallet;
    private final NotificationService notifications;
    private final RealtimePublisher realtime;
    private final AdminManagementService adminManagement;
    private final AdminAuditLogRepository audits;
    private final FileStorage fileStorage;
    private final AnalyticsEventService analytics;
    private final AdminAuthorizationService authorization;

    @Transactional(readOnly = true)
    public PageResult list(String keyword, String status, int page, int size) {
        String query = keyword == null ? "" : keyword.trim();
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String like = "%" + query + "%";
        Object[] parameters = {query, like, like, like, like, normalizedStatus, normalizedStatus};
        String where = " WHERE (?='' OR q.uid LIKE ? OR q.phone LIKE ? OR a.uid LIKE ? OR a.phone LIKE ?)" +
            " AND (?='' OR r.status=?)";
        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM inquiry_quality_reviews r " +
                "JOIN users q ON q.id=r.questioner_id JOIN users a ON a.id=r.answerer_id" + where,
            Long.class,
            parameters
        );
        List<Object> rowParameters = new java.util.ArrayList<>(List.of(parameters));
        rowParameters.add(size);
        rowParameters.add(page * size);
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT r.id,r.inquiry_id AS inquiryId,r.reason_code AS reasonCode,r.description," +
                "r.status,r.decision,r.decision_reason AS decisionReason,r.penalty_duration AS penaltyDuration," +
                "r.created_at AS createdAt,r.reviewed_at AS reviewedAt," +
                "q.uid AS questionerUid,q.phone AS questionerPhone," +
                "a.uid AS answererUid,a.phone AS answererPhone,i.question,i.amount " +
                "FROM inquiry_quality_reviews r JOIN inquiries i ON i.id=r.inquiry_id " +
                "JOIN users q ON q.id=r.questioner_id JOIN users a ON a.id=r.answerer_id" + where +
                " ORDER BY CASE WHEN r.status='PENDING' THEN 0 ELSE 1 END,r.id DESC LIMIT ? OFFSET ?",
            rowParameters.toArray()
        );
        return new PageResult(items, total == null ? 0 : total, page, size);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        return jdbc.queryForMap(
            "SELECT SUM(status='PENDING') AS pending," +
                "SUM(status='REFUNDED') AS refunded,SUM(status='SETTLED') AS settled," +
                "COUNT(*) AS total," +
                "(SELECT COUNT(*) FROM inquiry_evaluations) AS evaluationCount," +
                "(SELECT COUNT(*) FROM inquiry_evaluations WHERE answered_level=0 OR specific_level=0 " +
                "OR matched_level=0 OR useful_level=0 OR communication_level=0 OR ask_again_level=0) AS riskyEvaluationCount " +
                "FROM inquiry_quality_reviews"
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

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT r.id,r.inquiry_id AS inquiryId,r.reason_code AS reasonCode,r.description," +
                "r.status,r.decision,r.decision_reason AS decisionReason,r.penalty_duration AS penaltyDuration," +
                "r.created_at AS createdAt,r.reviewed_at AS reviewedAt," +
                "i.question,i.amount,i.answerer_income_amount AS answererIncomeAmount," +
                "i.service_fee_amount AS serviceFeeAmount,i.ended_at AS endedAt," +
                "q.id AS questionerId,q.uid AS questionerUid,q.phone AS questionerPhone," +
                "a.id AS answererId,a.uid AS answererUid,a.phone AS answererPhone," +
                "admin.display_name AS reviewedBy " +
                "FROM inquiry_quality_reviews r JOIN inquiries i ON i.id=r.inquiry_id " +
                "JOIN users q ON q.id=r.questioner_id JOIN users a ON a.id=r.answerer_id " +
                "LEFT JOIN admin_users admin ON admin.id=r.reviewed_by_admin_id WHERE r.id=?",
            id
        );
        if (rows.isEmpty()) throw BusinessException.notFound("质量复核不存在");
        Map<String, Object> result = new LinkedHashMap<>(rows.get(0));
        Long inquiryId = ((Number) result.get("inquiryId")).longValue();
        List<Map<String, Object>> evaluation = jdbc.queryForList(
            "SELECT answered_level AS answeredLevel,specific_level AS specificLevel," +
                "matched_level AS matchedLevel,useful_level AS usefulLevel," +
                "communication_level AS communicationLevel,ask_again_level AS askAgainLevel," +
                "negative_tags AS negativeTags,comment,created_at AS createdAt " +
                "FROM inquiry_evaluations WHERE inquiry_id=?",
            inquiryId
        );
        result.put("evaluation", evaluation.isEmpty() ? null : evaluation.get(0));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> evidence(Long id) {
        Map<String, Object> review = detail(id);
        Long inquiryId = ((Number) review.get("inquiryId")).longValue();
        return jdbc.queryForList(
            "SELECT m.id,m.sender_id AS senderId,u.uid AS senderUid,m.message_type AS messageType," +
                "m.content,m.attachment_key AS attachmentKey,m.attachment_url AS attachmentUrl," +
                "m.attachment_name AS attachmentName,m.created_at AS createdAt " +
                "FROM inquiry_messages m JOIN users u ON u.id=m.sender_id " +
                "WHERE m.inquiry_id=? ORDER BY m.id",
            inquiryId
        ).stream().map(this::withAttachmentUrl).toList();
    }

    @Transactional
    public Map<String, Object> resolve(
        AdminUser admin,
        Long id,
        String decision,
        String reason,
        String penaltyDuration,
        String ip
    ) {
        InquiryQualityReview review = reviews.findWithLockById(id)
            .orElseThrow(() -> BusinessException.notFound("质量复核不存在"));
        if (!"PENDING".equals(review.getStatus())) throw BusinessException.badRequest("该质量复核已经处理");
        String normalizedDecision = decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("REFUND", "SETTLE").contains(normalizedDecision)) {
            throw BusinessException.badRequest("请选择复核处理结果");
        }
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.isBlank()) throw BusinessException.badRequest("请填写处理理由");
        if (cleanReason.length() > 500) cleanReason = cleanReason.substring(0, 500);
        String penalty = penaltyDuration == null ? "NONE" : penaltyDuration.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("NONE", "DAYS_3", "DAYS_7", "DAYS_15", "PERMANENT").contains(penalty)) {
            throw BusinessException.badRequest("处罚时长不正确");
        }
        if ("SETTLE".equals(normalizedDecision) && !"NONE".equals(penalty)) {
            throw BusinessException.badRequest("维持结算时不能处罚回答者");
        }
        if (!"NONE".equals(penalty) && !authorization.hasPermission(admin, "ANSWER_QUALITY_PENALIZE")) {
            throw BusinessException.forbidden("当前账号没有执行处罚的权限");
        }

        Inquiry inquiry = review.getInquiry();
        if ("REFUND".equals(normalizedDecision)) {
            wallet.resolveQualityRefund(inquiry);
            inquiry.setStatus("QUALITY_REFUNDED");
            inquiry.setFundsStatus("REFUNDED");
            review.setStatus("REFUNDED");
            review.setDecision("FULL_REFUND");
        } else {
            wallet.resolveQualitySettlement(inquiry);
            inquiry.setStatus("COMPLETED");
            inquiry.setFundsStatus("SETTLED");
            review.setStatus("SETTLED");
            review.setDecision("NORMAL_SETTLEMENT");
        }
        review.setDecisionReason(cleanReason);
        review.setPenaltyDuration(penalty);
        review.setReviewedBy(admin);
        review.setReviewedAt(LocalDateTime.now());
        reviews.save(review);

        if (!"NONE".equals(penalty)) {
            adminManagement.userStatus(
                admin,
                review.getAnswerer().getId(),
                "SUSPENDED",
                penalty,
                "回答质量复核成立：" + cleanReason,
                ip
            );
        }
        String title = "REFUND".equals(normalizedDecision) ? "质量复核已退款" : "质量复核已完成";
        String questionerText = "REFUND".equals(normalizedDecision)
            ? "本次询问金额已全部退回账户余额"
            : "平台确认本次询问正常结算";
        String answererText = "REFUND".equals(normalizedDecision)
            ? "平台已将本次询问金额退回提问者"
            : "本次回答收入已完成结算";
        notifications.send(review.getQuestioner(), title, questionerText, "/inquiries/" + inquiry.getId());
        notifications.send(review.getAnswerer(), title, answererText, "/inquiries/" + inquiry.getId());
        realtime.afterCommit(review.getQuestioner().getId(), "INQUIRY_UPDATED", Map.of(
            "inquiryId", inquiry.getId(), "status", inquiry.getStatus(), "fundsStatus", inquiry.getFundsStatus()
        ));
        realtime.afterCommit(review.getAnswerer().getId(), "INQUIRY_UPDATED", Map.of(
            "inquiryId", inquiry.getId(), "status", inquiry.getStatus(), "fundsStatus", inquiry.getFundsStatus()
        ));
        audit(admin, "RESOLVE_ANSWER_QUALITY", "QUALITY_REVIEW", id,
            normalizedDecision + " | " + penalty + " | " + cleanReason, ip);
        analytics.recordBusinessAfterCommit(
            review.getQuestioner(),
            "REFUND".equals(normalizedDecision) ? "quality_review_refunded" : "quality_review_settled",
            analytics.properties("inquiry_id", inquiry.getId(), "penalty", penalty)
        );
        return detail(id);
    }

    private Map<String, Object> withAttachmentUrl(Map<String, Object> source) {
        Map<String, Object> item = new LinkedHashMap<>(source);
        String key = Objects.toString(item.remove("attachmentKey"), "");
        String legacy = Objects.toString(item.get("attachmentUrl"), "");
        if (!key.isBlank()) item.put("attachmentUrl", fileStorage.accessUrl(key, StorageVisibility.PRIVATE));
        else item.put("attachmentUrl", legacy);
        return item;
    }

    private void audit(AdminUser admin, String action, String targetType, Object targetId, String detail, String ip) {
        AdminAuditLog item = new AdminAuditLog();
        item.setAdminUser(admin);
        item.setAction(action);
        item.setTargetType(targetType);
        item.setTargetId(String.valueOf(targetId));
        item.setDetail(detail);
        item.setIpAddress(ip);
        audits.save(item);
    }

    public record PageResult(List<Map<String, Object>> items, long total, int page, int size) {
    }
}
