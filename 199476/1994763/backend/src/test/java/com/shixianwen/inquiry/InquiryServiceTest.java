package com.shixianwen.inquiry;

import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.network.ClientNetworkInfo;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.user.AnswererEligibilityService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InquiryServiceTest {
    @Test
    void creatingInquiryDoesNotRequireQuestionerCertification() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserRepository users = mock(UserRepository.class);
        AnswererEligibilityService eligibility = mock(AnswererEligibilityService.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        InquiryService service = new InquiryService(
            inquiries,
            mock(InquiryMessageRepository.class),
            users,
            mock(WalletService.class),
            mock(NotificationService.class),
            mock(RealtimePublisher.class),
            mock(FileStorage.class),
            eligibility,
            sensitiveWords
        );
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        when(users.findById(1L)).thenReturn(Optional.of(questioner));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));
        when(sensitiveWords.mask(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inquiries.save(any(Inquiry.class))).thenAnswer(invocation -> {
            Inquiry saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });

        InquiryService.InquiryView result = service.create(
            1L,
            new InquiryService.CreateCommand(
                2L,
                "水暖工",
                "PROFILE",
                "家里漏水该怎么处理",
                new BigDecimal("10")
            ),
            new ClientNetworkInfo("127.0.0.1", "内网")
        );

        assertEquals(9L, result.id());
        verify(eligibility).requireAvailable(2L);
        verify(eligibility, never()).requireAvailable(1L);
        verify(eligibility, never()).requireQualified(1L);
    }

    private User user(Long id, String uid) {
        User user = new User();
        user.setId(id);
        user.setUid(uid);
        user.setPhone(uid);
        return user;
    }
}
