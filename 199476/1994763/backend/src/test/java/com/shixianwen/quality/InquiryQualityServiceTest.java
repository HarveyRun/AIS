package com.shixianwen.quality;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.inquiry.Inquiry;
import com.shixianwen.inquiry.InquiryRepository;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.user.User;
import com.shixianwen.wallet.WalletIncomeHoldRepository;
import com.shixianwen.wallet.WalletService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InquiryQualityServiceTest {
    @Test
    void onlyQuestionerCanEvaluate() {
        Dependencies dependencies = new Dependencies();
        Inquiry inquiry = completedInquiry();
        when(dependencies.inquiries.findWithLockById(8L)).thenReturn(Optional.of(inquiry));

        assertThatThrownBy(() -> dependencies.service().evaluate(
            2L,
            8L,
            new InquiryQualityService.EvaluationCommand(2, 2, 2, 2, 2, 2, List.of(), null)
        )).hasMessageContaining("只有提问者");
    }

    @Test
    void requestingQualityRefundIsNoLongerAvailable() {
        Dependencies dependencies = new Dependencies();
        assertThatThrownBy(() -> dependencies.service().requestReview(
            1L,
            8L,
            new InquiryQualityService.ReviewCommand("NO_EFFECTIVE_ANSWER", "没有回答我的问题")
        )).hasMessageContaining("不支持申请质量退款");
        verifyNoInteractions(dependencies.wallet);
    }

    private static Inquiry completedInquiry() {
        User questioner = new User();
        questioner.setId(1L);
        User answerer = new User();
        answerer.setId(2L);
        Inquiry inquiry = new Inquiry();
        inquiry.setId(8L);
        inquiry.setQuestioner(questioner);
        inquiry.setAnswerer(answerer);
        inquiry.setStatus("COMPLETED");
        inquiry.setFundsStatus("SETTLED");
        inquiry.setEndedAt(LocalDateTime.now());
        return inquiry;
    }

    private static final class Dependencies {
        private final InquiryRepository inquiries = mock(InquiryRepository.class);
        private final InquiryEvaluationRepository evaluations = mock(InquiryEvaluationRepository.class);
        private final InquiryQualityReviewRepository reviews = mock(InquiryQualityReviewRepository.class);
        private final WalletIncomeHoldRepository holds = mock(WalletIncomeHoldRepository.class);
        private final WalletService wallet = mock(WalletService.class);
        private final NotificationService notifications = mock(NotificationService.class);
        private final SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        private final AnalyticsEventService analytics = mock(AnalyticsEventService.class);
        private final RealtimePublisher realtime = mock(RealtimePublisher.class);

        private InquiryQualityService service() {
            when(sensitiveWords.mask(any())).thenAnswer(invocation -> invocation.getArgument(0));
            return new InquiryQualityService(
                inquiries,
                evaluations,
                reviews,
                holds,
                wallet,
                notifications,
                sensitiveWords,
                new ObjectMapper(),
                analytics,
                realtime
            );
        }
    }
}
