package com.shixianwen.quality;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.common.BusinessException;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.inquiry.Inquiry;
import com.shixianwen.inquiry.InquiryRepository;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.wallet.WalletIncomeHold;
import com.shixianwen.wallet.WalletIncomeHoldRepository;
import com.shixianwen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InquiryQualityService {
    private static final Set<String> NEGATIVE_TAGS = Set.of(
        "OFF_TOPIC", "TOO_GENERAL", "NO_RESPONSE", "SUSPECTED_FABRICATION",
        "JOB_MISMATCH", "INAPPROPRIATE_LANGUAGE", "OFF_PLATFORM_PAYMENT",
        "ADVERTISEMENT", "OTHER"
    );
    private static final Set<String> REVIEW_REASONS = Set.of(
        "NO_EFFECTIVE_ANSWER", "CLEARLY_OFF_TOPIC", "SUSPECTED_FABRICATION",
        "JOB_MISMATCH", "HARASSMENT", "OFF_PLATFORM_PAYMENT", "OTHER"
    );

    private final InquiryRepository inquiries;
    private final InquiryEvaluationRepository evaluations;
    private final InquiryQualityReviewRepository reviews;
    private final WalletIncomeHoldRepository incomeHolds;
    private final WalletService wallet;
    private final NotificationService notifications;
    private final SensitiveWordService sensitiveWords;
    private final ObjectMapper objectMapper;
    private final AnalyticsEventService analytics;
    private final RealtimePublisher realtime;

    @Transactional(readOnly = true)
    public QualityOptions options(Long userId, Long inquiryId) {
        Inquiry inquiry = participant(userId, inquiryId);
        boolean questioner = inquiry.getQuestioner().getId().equals(userId);
        boolean completed = Set.of("COMPLETED", "QUALITY_REFUNDED").contains(inquiry.getStatus());
        boolean evaluated = evaluations.existsByInquiryId(inquiryId);
        InquiryQualityReview review = reviews.findByInquiryId(inquiryId).orElse(null);
        WalletIncomeHold hold = incomeHolds.findByInquiryId(inquiryId).orElse(null);
        boolean reviewAvailable = false;
        boolean evaluationAvailable = questioner && completed && !evaluated
            && inquiry.getEndedAt() != null
            && inquiry.getEndedAt().plusDays(7).isAfter(LocalDateTime.now());
        return new QualityOptions(
            evaluationAvailable,
            evaluated,
            reviewAvailable,
            hold == null ? null : hold.getReleaseAt(),
            review == null ? null : review.getStatus(),
            review == null ? null : review.getDecision(),
            review == null ? null : review.getDecisionReason()
        );
    }

    @Transactional
    public EvaluationView evaluate(Long userId, Long inquiryId, EvaluationCommand command) {
        Inquiry inquiry = inquiries.findWithLockById(inquiryId)
            .orElseThrow(() -> BusinessException.notFound("询问不存在"));
        if (!inquiry.getQuestioner().getId().equals(userId)) {
            throw BusinessException.forbidden("只有提问者可以评价本次交流");
        }
        if (!Set.of("COMPLETED", "QUALITY_REFUNDED").contains(inquiry.getStatus())
            || inquiry.getEndedAt() == null
            || !inquiry.getEndedAt().plusDays(7).isAfter(LocalDateTime.now())) {
            throw BusinessException.badRequest("当前不能评价本次交流");
        }
        if (evaluations.existsByInquiryId(inquiryId)) {
            throw BusinessException.badRequest("本次交流已经评价");
        }
        validateLevel(command.answeredLevel());
        validateLevel(command.specificLevel());
        validateLevel(command.matchedLevel());
        validateLevel(command.usefulLevel());
        validateLevel(command.communicationLevel());
        validateLevel(command.askAgainLevel());
        List<String> tags = normalizedTags(command.negativeTags());
        String comment = optional(command.comment(), 300);

        InquiryEvaluation item = new InquiryEvaluation();
        item.setInquiry(inquiry);
        item.setQuestioner(inquiry.getQuestioner());
        item.setAnsweredLevel(command.answeredLevel());
        item.setSpecificLevel(command.specificLevel());
        item.setMatchedLevel(command.matchedLevel());
        item.setUsefulLevel(command.usefulLevel());
        item.setCommunicationLevel(command.communicationLevel());
        item.setAskAgainLevel(command.askAgainLevel());
        item.setNegativeTags(json(tags));
        item.setComment(comment);
        item = evaluations.save(item);
        analytics.recordBusinessAfterCommit(inquiry.getQuestioner(), "inquiry_evaluated", analytics.properties(
            "inquiry_id", inquiryId,
            "has_negative_tag", !tags.isEmpty()
        ));
        return EvaluationView.from(item, tags);
    }

    @Transactional
    public QualityReviewView requestReview(Long userId, Long inquiryId, ReviewCommand command) {
        throw BusinessException.badRequest("当前不支持申请质量退款");
    }

    private Inquiry participant(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiries.findById(inquiryId)
            .orElseThrow(() -> BusinessException.notFound("询问不存在"));
        if (!inquiry.getQuestioner().getId().equals(userId)
            && !inquiry.getAnswerer().getId().equals(userId)) {
            throw BusinessException.forbidden("无权查看该询问");
        }
        return inquiry;
    }

    private void validateLevel(int level) {
        if (level < 0 || level > 2) throw BusinessException.badRequest("评价选项不正确");
    }

    private List<String> normalizedTags(List<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        values.stream().limit(8).forEach(value -> {
            String normalized = value == null ? "" : value.trim().toUpperCase();
            if (NEGATIVE_TAGS.contains(normalized)) result.add(normalized);
        });
        return List.copyOf(result);
    }

    private String required(String value, int max, String message) {
        if (value == null || value.isBlank()) throw BusinessException.badRequest(message);
        String clean = sensitiveWords.mask(value.trim());
        return clean.substring(0, Math.min(clean.length(), max));
    }

    private String optional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String clean = sensitiveWords.mask(value.trim());
        return clean.substring(0, Math.min(clean.length(), max));
    }

    private String json(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    public record EvaluationCommand(
        int answeredLevel,
        int specificLevel,
        int matchedLevel,
        int usefulLevel,
        int communicationLevel,
        int askAgainLevel,
        List<String> negativeTags,
        String comment
    ) {
    }

    public record ReviewCommand(String reasonCode, String description) {
    }

    public record QualityOptions(
        boolean canEvaluate,
        boolean evaluated,
        boolean canRequestReview,
        LocalDateTime reviewDeadline,
        String reviewStatus,
        String reviewDecision,
        String reviewDecisionReason
    ) {
    }

    public record EvaluationView(
        Long id,
        Long inquiryId,
        int answeredLevel,
        int specificLevel,
        int matchedLevel,
        int usefulLevel,
        int communicationLevel,
        int askAgainLevel,
        List<String> negativeTags,
        String comment,
        LocalDateTime createdAt
    ) {
        static EvaluationView from(InquiryEvaluation item, List<String> tags) {
            return new EvaluationView(
                item.getId(), item.getInquiry().getId(), item.getAnsweredLevel(), item.getSpecificLevel(),
                item.getMatchedLevel(), item.getUsefulLevel(), item.getCommunicationLevel(),
                item.getAskAgainLevel(), tags, item.getComment(), item.getCreatedAt()
            );
        }
    }

    public record QualityReviewView(
        Long id,
        Long inquiryId,
        String reasonCode,
        String description,
        String status,
        String decision,
        String decisionReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
    ) {
        static QualityReviewView from(InquiryQualityReview item) {
            return new QualityReviewView(
                item.getId(), item.getInquiry().getId(), item.getReasonCode(), item.getDescription(),
                item.getStatus(), item.getDecision(), item.getDecisionReason(), item.getCreatedAt(), item.getReviewedAt()
            );
        }
    }
}
