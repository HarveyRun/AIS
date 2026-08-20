package com.shixianwen.inquiry;

import com.shixianwen.common.BusinessException;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.network.ClientNetworkInfo;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.user.AnswererEligibilityService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletService;
import com.shixianwen.security.SecurityEventService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InquiryServiceTest {
    @Test
    void unrelatedUserCannotReadAnotherInquiry() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        Inquiry inquiry = new Inquiry();
        inquiry.setId(11L);
        inquiry.setQuestioner(user(1L, "1000001"));
        inquiry.setAnswerer(user(2L, "2000002"));
        when(inquiries.findById(11L)).thenReturn(Optional.of(inquiry));
        InquiryService service = new InquiryService(
            inquiries,
            mock(InquiryMessageRepository.class),
            mock(UserRepository.class),
            mock(WalletService.class),
            mock(NotificationService.class),
            mock(RealtimePublisher.class),
            mock(FileStorage.class),
            mock(AnswererEligibilityService.class),
            mock(SensitiveWordService.class),
            mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class)
        );

        assertThrows(BusinessException.class, () -> service.detail(3L, 11L));
    }

    @Test
    void creatingInquiryDoesNotRequireQuestionerCertification() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserRepository users = mock(UserRepository.class);
        AnswererEligibilityService eligibility = mock(AnswererEligibilityService.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        WalletService wallet = mock(WalletService.class);
        InquiryService service = new InquiryService(
            inquiries,
            mock(InquiryMessageRepository.class),
            users,
            wallet,
            mock(NotificationService.class),
            mock(RealtimePublisher.class),
            mock(FileStorage.class),
            eligibility,
            sensitiveWords,
            mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class)
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
        when(wallet.freeze(any(), any(), any()))
            .thenReturn(new WalletService.FrozenAllocation(new BigDecimal("10.00"), BigDecimal.ZERO));
        when(wallet.quoteInquirySettlement(any(), any())).thenReturn(
            new com.shixianwen.wallet.PlatformServiceFeePolicy.SettlementQuote(
                "ANDROID", new BigDecimal("10.00"), new BigDecimal("0.050000"),
                new BigDecimal("0.50"), new BigDecimal("9.50")
            )
        );

        InquiryService.InquiryView result = service.create(
            1L,
            new InquiryService.CreateCommand(
                2L,
                "水暖工",
                "PROFILE",
                "家里漏水该怎么处理",
                new BigDecimal("10"),
                "android"
            ),
            new ClientNetworkInfo("127.0.0.1", "内网")
        );

        assertEquals(9L, result.id());
        assertEquals(new BigDecimal("9.50"), result.answererIncomeAmount());
        assertEquals(new BigDecimal("0.50"), result.serviceFeeAmount());
        verify(eligibility).requireAvailable(2L);
        verify(eligibility, never()).requireAvailable(1L);
        verify(eligibility, never()).requireQualified(1L);
    }

    @Test
    void creatingInquiryRejectsAmountOutsideAnswererRangeBeforeFreezingFunds() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        InquiryService service = new InquiryService(
            inquiries,
            mock(InquiryMessageRepository.class),
            users,
            wallet,
            mock(NotificationService.class),
            mock(RealtimePublisher.class),
            mock(FileStorage.class),
            mock(AnswererEligibilityService.class),
            mock(SensitiveWordService.class),
            mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class)
        );
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        answerer.setInquiryPriceMin(50);
        answerer.setInquiryPriceMax(200);
        when(users.findById(1L)).thenReturn(Optional.of(questioner));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));

        assertThrows(
            BusinessException.class,
            () -> service.create(
                1L,
                new InquiryService.CreateCommand(
                    2L,
                    "水暖工",
                    "PROFILE",
                    "家里漏水该怎么处理",
                    new BigDecimal("20"),
                    "android"
                ),
                new ClientNetworkInfo("127.0.0.1", "内网")
            )
        );
        verify(wallet, never()).freeze(any(), any(), any());
    }

    @Test
    void testAccountsCanCreateSandboxInquiriesWithEachOther() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        AnswererEligibilityService eligibility = mock(AnswererEligibilityService.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        InquiryService service = new InquiryService(
            inquiries,
            mock(InquiryMessageRepository.class),
            users,
            wallet,
            mock(NotificationService.class),
            mock(RealtimePublisher.class),
            mock(FileStorage.class),
            eligibility,
            sensitiveWords,
            mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class)
        );
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        questioner.setAccountType("TEST");
        answerer.setAccountType("TEST");
        when(users.findById(1L)).thenReturn(Optional.of(questioner));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));
        when(sensitiveWords.mask(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(wallet.freeze(any(), any(), any()))
            .thenReturn(new WalletService.FrozenAllocation(new BigDecimal("10.00"), BigDecimal.ZERO));
        when(wallet.quoteInquirySettlement(any(), any())).thenReturn(
            new com.shixianwen.wallet.PlatformServiceFeePolicy.SettlementQuote(
                "ANDROID", new BigDecimal("10.00"), new BigDecimal("0.050000"),
                new BigDecimal("0.50"), new BigDecimal("9.50")
            )
        );
        when(inquiries.save(any(Inquiry.class))).thenAnswer(invocation -> {
            Inquiry item = invocation.getArgument(0);
            item.setId(90L);
            return item;
        });

        InquiryService.InquiryView result = service.create(
            1L,
            new InquiryService.CreateCommand(
                2L, "水暖工", "PROFILE", "测试询问", new BigDecimal("10"), "android"
            ),
            new ClientNetworkInfo("127.0.0.1", "内网")
        );

        assertEquals(90L, result.id());
        verify(wallet).freeze(1L, new BigDecimal("10.00"), 90L);
    }

    @Test
    void testAndNormalAccountsCannotCreateCrossEnvironmentInquiries() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        InquiryService service = new InquiryService(
            inquiries,
            mock(InquiryMessageRepository.class),
            users,
            wallet,
            mock(NotificationService.class),
            mock(RealtimePublisher.class),
            mock(FileStorage.class),
            mock(AnswererEligibilityService.class),
            mock(SensitiveWordService.class),
            mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class)
        );
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        questioner.setAccountType("TEST");
        when(users.findById(1L)).thenReturn(Optional.of(questioner));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));

        assertThrows(
            BusinessException.class,
            () -> service.create(
                1L,
                new InquiryService.CreateCommand(
                    2L, "水暖工", "PROFILE", "测试询问", new BigDecimal("10"), "android"
                ),
                new ClientNetworkInfo("127.0.0.1", "内网")
            )
        );
        verify(wallet, never()).freeze(any(), any(), any());
    }

    private User user(Long id, String uid) {
        User user = new User();
        user.setId(id);
        user.setUid(uid);
        user.setPhone(uid);
        return user;
    }
}
