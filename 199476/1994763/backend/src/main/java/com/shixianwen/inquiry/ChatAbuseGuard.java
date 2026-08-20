package com.shixianwen.inquiry;

import com.shixianwen.common.BusinessException;
import com.shixianwen.security.SecurityEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatAbuseGuard {
    private final InquiryMessageRepository messages;
    private final SecurityEventService securityEvents;

    public void requireTextAllowed(Long inquiryId, Long userId, String content) {
        LocalDateTime now = LocalDateTime.now();
        requireGeneralAllowed(inquiryId, userId, now);
        if (messages.countByInquiryIdAndSenderIdAndContentAndCreatedAtAfter(
            inquiryId, userId, content, now.minusMinutes(1)
        ) >= 5) block(userId, inquiryId, "短时间重复发送相同内容");
    }

    public void requireImageAllowed(Long inquiryId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        requireGeneralAllowed(inquiryId, userId, now);
        if (messages.countByInquiryIdAndSenderIdAndMessageTypeAndCreatedAtAfter(
            inquiryId, userId, "IMAGE", now.minusMinutes(10)
        ) >= 5) block(userId, inquiryId, "10分钟内图片发送过多");
    }

    private void requireGeneralAllowed(Long inquiryId, Long userId, LocalDateTime now) {
        if (messages.countByInquiryIdAndSenderIdAndCreatedAtAfter(
            inquiryId, userId, now.minusMinutes(1)
        ) >= 20) block(userId, inquiryId, "1分钟内消息发送过多");
        if (messages.countByInquiryIdAndSenderIdAndCreatedAtAfter(
            inquiryId, userId, now.minusDays(1)
        ) >= 500) block(userId, inquiryId, "24小时内消息发送过多");
    }

    private void block(Long userId, Long inquiryId, String reason) {
        securityEvents.recordSafely(
            userId, null, "CHAT_ABUSE_BLOCKED", "HIGH", null, null,
            "inquiryId=" + inquiryId + ", reason=" + reason
        );
        throw BusinessException.tooManyRequests("发送过于频繁，请稍后再试");
    }
}
