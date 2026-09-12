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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InquiryServiceTest {
    @Test
    void thirdInquiryStillCreatesFreeTextInquiryWithoutFreezingFunds() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        InquiryMessageRepository messages = mock(InquiryMessageRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        org.springframework.jdbc.core.JdbcTemplate jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        InquiryService service = new InquiryService(
            inquiries, messages, users, wallet, mock(NotificationService.class),
            mock(RealtimePublisher.class), mock(FileStorage.class),
            mock(AnswererEligibilityService.class), sensitiveWords,
            mock(com.shixianwen.content.SensitiveContentCipher.class),
            mock(ChatAbuseGuard.class), mock(SecurityEventService.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class), jdbc,
            mock(UserCommunicationBlockService.class)
        );
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        answerer.setInquiryHourlyRate(180);
        when(users.findById(1L)).thenReturn(Optional.of(questioner));
        when(users.findWithLockById(1L)).thenReturn(Optional.of(questioner));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));
        when(users.findWithLockById(2L)).thenReturn(Optional.of(answerer));
        when(inquiries.countByQuestionerIdAndStatus(1L, "PENDING")).thenReturn(2L);
        when(sensitiveWords.mask(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inquiries.save(any(Inquiry.class))).thenAnswer(invocation -> {
            Inquiry saved = invocation.getArgument(0);
            saved.setId(91L);
            return saved;
        });
        when(jdbc.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
            .thenReturn(java.util.List.of(new InquiryService.InquiryExperience("装修经历")));

        InquiryService.InquiryView result = service.create(
            1L,
            new InquiryService.CreateCommand(2L, 81L, "我正在装修新房", "android"),
            new ClientNetworkInfo("127.0.0.1", "内网")
        );

        assertEquals(2, result.flowVersion());
        assertEquals(BigDecimal.ZERO.setScale(2), result.amount());
        assertEquals("我正在装修新房", result.question());
        assertEquals(180, result.hourlyRateSnapshot());
        assertTrue(result.responseDeadline().isAfter(LocalDateTime.now().plusHours(71)));
        assertTrue(result.responseDeadline().isBefore(LocalDateTime.now().plusHours(73)));
        verify(wallet, never()).freeze(any(), any(), any());
    }

    @Test
    void fourthInquiryFreezesTwoYuanDeposit() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        org.springframework.jdbc.core.JdbcTemplate jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        InquiryService service = new InquiryService(
            inquiries, mock(InquiryMessageRepository.class), users, wallet,
            mock(NotificationService.class), mock(RealtimePublisher.class), mock(FileStorage.class),
            mock(AnswererEligibilityService.class), sensitiveWords,
            mock(com.shixianwen.content.SensitiveContentCipher.class), mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class), mock(com.shixianwen.analytics.AnalyticsEventService.class), jdbc,
            mock(UserCommunicationBlockService.class)
        );
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        when(users.findWithLockById(1L)).thenReturn(Optional.of(questioner));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));
        when(users.findWithLockById(2L)).thenReturn(Optional.of(answerer));
        when(inquiries.countByQuestionerIdAndStatus(1L, "PENDING")).thenReturn(3L);
        when(sensitiveWords.mask(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inquiries.save(any(Inquiry.class))).thenAnswer(invocation -> {
            Inquiry saved = invocation.getArgument(0);
            saved.setId(92L);
            return saved;
        });
        when(wallet.freezeInquiryDeposit(1L, new BigDecimal("2.00"), 92L))
            .thenReturn(new WalletService.FrozenAllocation(new BigDecimal("2.00"), BigDecimal.ZERO));
        when(jdbc.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
            .thenReturn(java.util.List.of(new InquiryService.InquiryExperience("装修经历")));

        InquiryService.InquiryView result = service.create(
            1L,
            new InquiryService.CreateCommand(2L, 81L, "我正在装修新房", "android"),
            new ClientNetworkInfo("127.0.0.1", "内网")
        );

        assertEquals(new BigDecimal("2.00"), result.depositAmount());
        assertEquals("FROZEN", result.depositStatus());
        verify(wallet).freezeInquiryDeposit(1L, new BigDecimal("2.00"), 92L);
    }

    @Test
    void questionerWithActiveInquiryCannotCreateAnotherInquiry() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserRepository users = mock(UserRepository.class);
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        when(users.findWithLockById(1L)).thenReturn(Optional.of(questioner));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));
        when(users.findWithLockById(2L)).thenReturn(Optional.of(answerer));
        when(inquiries.countByQuestionerIdAndStatusIn(any(), any())).thenReturn(1L);
        InquiryService service = new InquiryService(
            inquiries, mock(InquiryMessageRepository.class), users, mock(WalletService.class),
            mock(NotificationService.class), mock(RealtimePublisher.class), mock(FileStorage.class),
            mock(AnswererEligibilityService.class), mock(SensitiveWordService.class),
            mock(com.shixianwen.content.SensitiveContentCipher.class), mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class), mock(com.shixianwen.analytics.AnalyticsEventService.class),
            mock(org.springframework.jdbc.core.JdbcTemplate.class),
            mock(UserCommunicationBlockService.class)
        );

        BusinessException error = assertThrows(
            BusinessException.class,
            () -> service.create(
                1L,
                new InquiryService.CreateCommand(2L, 81L, "我正在装修新房", "android"),
                new ClientNetworkInfo("127.0.0.1", "内网")
            )
        );

        assertEquals("你同时进行的询问已达1条，结束后才能发起新的询问", error.getMessage());
    }

    @Test
    void answererWithActiveInquiryCannotAcceptAnotherInquiry() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserRepository users = mock(UserRepository.class);
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        Inquiry pending = new Inquiry();
        pending.setId(93L);
        pending.setQuestioner(questioner);
        pending.setAnswerer(answerer);
        pending.setStatus("PENDING");
        when(inquiries.findWithLockById(93L)).thenReturn(Optional.of(pending));
        when(users.findWithLockById(1L)).thenReturn(Optional.of(questioner));
        when(users.findWithLockById(2L)).thenReturn(Optional.of(answerer));
        when(inquiries.countByAnswererIdAndStatusIn(any(), any())).thenReturn(3L);
        InquiryService service = new InquiryService(
            inquiries, mock(InquiryMessageRepository.class), users, mock(WalletService.class),
            mock(NotificationService.class), mock(RealtimePublisher.class), mock(FileStorage.class),
            mock(AnswererEligibilityService.class), mock(SensitiveWordService.class),
            mock(com.shixianwen.content.SensitiveContentCipher.class), mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class), mock(com.shixianwen.analytics.AnalyticsEventService.class),
            mock(org.springframework.jdbc.core.JdbcTemplate.class),
            mock(UserCommunicationBlockService.class)
        );

        BusinessException error = assertThrows(
            BusinessException.class,
            () -> service.accept(2L, 93L)
        );

        assertEquals("对方同时接受的询问已达3条，请稍后再试", error.getMessage());
    }

    @Test
    void cancellingPendingInquiryReturnsDeposit() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        Inquiry pending = new Inquiry();
        pending.setId(94L);
        pending.setQuestioner(questioner);
        pending.setAnswerer(answerer);
        pending.setStatus("PENDING");
        pending.setFlowVersion(2);
        pending.setDepositAmount(new BigDecimal("2.00"));
        pending.setDepositFrozenRechargeAmount(new BigDecimal("2.00"));
        pending.setDepositFrozenIncomeAmount(BigDecimal.ZERO.setScale(2));
        pending.setDepositStatus("FROZEN");
        when(inquiries.findWithLockById(94L)).thenReturn(Optional.of(pending));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));
        InquiryService service = new InquiryService(
            inquiries, mock(InquiryMessageRepository.class), users, wallet,
            mock(NotificationService.class), mock(RealtimePublisher.class), mock(FileStorage.class),
            mock(AnswererEligibilityService.class), mock(SensitiveWordService.class),
            mock(com.shixianwen.content.SensitiveContentCipher.class), mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class), mock(com.shixianwen.analytics.AnalyticsEventService.class),
            mock(org.springframework.jdbc.core.JdbcTemplate.class),
            mock(UserCommunicationBlockService.class)
        );

        InquiryService.InquiryView result = service.cancel(1L, 94L);

        assertEquals("CANCELLED", result.status());
        assertEquals("REFUNDED", result.depositStatus());
        verify(wallet).refundInquiryDeposit(
            1L,
            new BigDecimal("2.00"),
            BigDecimal.ZERO.setScale(2),
            94L
        );
    }

    @Test
    void questionerEndingCurrentInquiryClosesTheWholeInquiryAndReturnsDeposit() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        InquiryMessageRepository messages = mock(InquiryMessageRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        Inquiry active = new Inquiry();
        active.setId(95L);
        active.setQuestioner(questioner);
        active.setAnswerer(answerer);
        active.setQuestion("我想问询这段经历");
        active.setAmount(BigDecimal.ZERO.setScale(2));
        active.setStatus("ACTIVE");
        active.setFundsStatus("NONE");
        active.setFlowVersion(2);
        active.setDepositAmount(new BigDecimal("2.00"));
        active.setDepositFrozenRechargeAmount(new BigDecimal("2.00"));
        active.setDepositFrozenIncomeAmount(BigDecimal.ZERO.setScale(2));
        active.setDepositStatus("FROZEN");
        when(inquiries.findWithLockById(95L)).thenReturn(Optional.of(active));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));
        when(messages.saveAndFlush(any(InquiryMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        InquiryService service = new InquiryService(
            inquiries, messages, users, wallet,
            mock(NotificationService.class), mock(RealtimePublisher.class), mock(FileStorage.class),
            mock(AnswererEligibilityService.class), mock(SensitiveWordService.class),
            mock(com.shixianwen.content.SensitiveContentCipher.class), mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class), mock(com.shixianwen.analytics.AnalyticsEventService.class),
            mock(org.springframework.jdbc.core.JdbcTemplate.class),
            mock(UserCommunicationBlockService.class)
        );

        InquiryService.InquiryView result = service.endInquiry(1L, 95L);

        assertEquals("COMPLETED", result.status());
        assertEquals("REFUNDED", result.depositStatus());
        assertTrue(active.getEndedAt() != null);
        verify(wallet).refundInquiryDeposit(
            1L,
            new BigDecimal("2.00"),
            BigDecimal.ZERO.setScale(2),
            95L
        );
        org.mockito.ArgumentCaptor<InquiryMessage> messageCaptor =
            org.mockito.ArgumentCaptor.forClass(InquiryMessage.class);
        verify(messages).saveAndFlush(messageCaptor.capture());
        assertEquals("提问者已结束本次询问。", messageCaptor.getValue().getContent());
    }

    @Test
    void pendingInquiryDoesNotAllowQuestionerToSendMessages() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        Inquiry inquiry = new Inquiry();
        inquiry.setId(91L);
        inquiry.setQuestioner(user(1L, "1000001"));
        inquiry.setAnswerer(user(2L, "2000002"));
        inquiry.setStatus("PENDING");
        when(inquiries.findWithLockById(91L)).thenReturn(Optional.of(inquiry));
        InquiryService service = new InquiryService(
            inquiries, mock(InquiryMessageRepository.class), mock(UserRepository.class),
            mock(WalletService.class), mock(NotificationService.class), mock(RealtimePublisher.class),
            mock(FileStorage.class), mock(AnswererEligibilityService.class), mock(SensitiveWordService.class),
            mock(com.shixianwen.content.SensitiveContentCipher.class), mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class), mock(com.shixianwen.analytics.AnalyticsEventService.class),
            mock(org.springframework.jdbc.core.JdbcTemplate.class),
            mock(UserCommunicationBlockService.class)
        );

        BusinessException error = assertThrows(
            BusinessException.class,
            () -> service.send(1L, 91L, "你好")
        );

        assertEquals("当前状态不能发送消息", error.getMessage());
    }

    @Test
    void textSentDuringVoiceCallStillConsumesTheFreeMessageQuota() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        InquiryMessageRepository messages = mock(InquiryMessageRepository.class);
        UserRepository users = mock(UserRepository.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        com.shixianwen.content.SensitiveContentCipher cipher =
            mock(com.shixianwen.content.SensitiveContentCipher.class);
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        Inquiry inquiry = new Inquiry();
        inquiry.setId(96L);
        inquiry.setQuestioner(questioner);
        inquiry.setAnswerer(answerer);
        inquiry.setStatus("ACTIVE");
        inquiry.setFlowVersion(2);
        when(inquiries.findWithLockById(96L)).thenReturn(Optional.of(inquiry));
        when(users.findById(1L)).thenReturn(Optional.of(questioner));
        when(sensitiveWords.mask("语音期间补充文字")).thenReturn("语音期间补充文字");
        when(cipher.encrypt("语音期间补充文字")).thenReturn("encrypted");
        when(messages.saveAndFlush(any(InquiryMessage.class))).thenAnswer(invocation -> {
            InquiryMessage saved = invocation.getArgument(0);
            saved.setId(501L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });
        InquiryService service = new InquiryService(
            inquiries, messages, users, mock(WalletService.class),
            mock(NotificationService.class), mock(RealtimePublisher.class), mock(FileStorage.class),
            mock(AnswererEligibilityService.class), sensitiveWords, cipher,
            mock(ChatAbuseGuard.class), mock(SecurityEventService.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class),
            mock(org.springframework.jdbc.core.JdbcTemplate.class),
            mock(UserCommunicationBlockService.class)
        );

        service.send(1L, 96L, "语音期间补充文字");

        org.mockito.ArgumentCaptor<InquiryMessage> messageCaptor =
            org.mockito.ArgumentCaptor.forClass(InquiryMessage.class);
        verify(messages).saveAndFlush(messageCaptor.capture());
        assertEquals(true, messageCaptor.getValue().isCountsTowardFreeLimit());
    }

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
            mock(com.shixianwen.content.SensitiveContentCipher.class),
            mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class),
            mock(org.springframework.jdbc.core.JdbcTemplate.class),
            mock(UserCommunicationBlockService.class)
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
        org.springframework.jdbc.core.JdbcTemplate jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
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
            mock(com.shixianwen.content.SensitiveContentCipher.class),
            mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class),
            jdbc,
            mock(UserCommunicationBlockService.class)
        );
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        when(users.findById(1L)).thenReturn(Optional.of(questioner));
        when(users.findWithLockById(1L)).thenReturn(Optional.of(questioner));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));
        when(users.findWithLockById(2L)).thenReturn(Optional.of(answerer));
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
        when(jdbc.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
            .thenReturn(java.util.List.of(
                new InquiryService.InquiryExperience("装修经历")
            ));

        InquiryService.InquiryView result = service.create(
            1L,
            new InquiryService.CreateCommand(
                2L,
                81L,
                "我正在装修新房",
                "android"
            ),
            new ClientNetworkInfo("127.0.0.1", "内网")
        );

        assertEquals(9L, result.id());
        assertEquals(BigDecimal.ZERO.setScale(2), result.amount());
        assertEquals("NONE", result.fundsStatus());
        verify(eligibility).requireAvailable(2L);
        verify(eligibility, never()).requireAvailable(1L);
        verify(eligibility, never()).requireQualified(1L);
    }

    @Test
    void unavailableExperienceCannotBeUsedToCreateInquiry() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        org.springframework.jdbc.core.JdbcTemplate jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
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
            mock(com.shixianwen.content.SensitiveContentCipher.class),
            mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class),
            jdbc,
            mock(UserCommunicationBlockService.class)
        );
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        when(users.findById(1L)).thenReturn(Optional.of(questioner));
        when(users.findWithLockById(1L)).thenReturn(Optional.of(questioner));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));
        when(users.findWithLockById(2L)).thenReturn(Optional.of(answerer));
        when(jdbc.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
            .thenReturn(java.util.List.of());

        BusinessException error = assertThrows(
            BusinessException.class,
            () -> service.create(
                1L,
                new InquiryService.CreateCommand(
                    2L, 82L, "我正在装修新房", "android"
                ),
                new ClientNetworkInfo("127.0.0.1", "内网")
            )
        );

        assertEquals("这段亲身经历已不可询问", error.getMessage());
        verify(wallet, never()).freeze(any(), any(), any());
    }

    @Test
    void testAccountsCanCreateSandboxInquiriesWithEachOther() {
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        AnswererEligibilityService eligibility = mock(AnswererEligibilityService.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        org.springframework.jdbc.core.JdbcTemplate jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
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
            mock(com.shixianwen.content.SensitiveContentCipher.class),
            mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class),
            jdbc,
            mock(UserCommunicationBlockService.class)
        );
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        questioner.setAccountType("TEST");
        answerer.setAccountType("TEST");
        when(users.findById(1L)).thenReturn(Optional.of(questioner));
        when(users.findWithLockById(1L)).thenReturn(Optional.of(questioner));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));
        when(users.findWithLockById(2L)).thenReturn(Optional.of(answerer));
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
        when(jdbc.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
            .thenReturn(java.util.List.of(
                new InquiryService.InquiryExperience("装修经历")
            ));

        InquiryService.InquiryView result = service.create(
            1L,
            new InquiryService.CreateCommand(
                2L, 81L, "我正在装修新房", "android"
            ),
            new ClientNetworkInfo("127.0.0.1", "内网")
        );

        assertEquals(90L, result.id());
        verify(wallet, never()).freeze(any(), any(), any());
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
            mock(com.shixianwen.content.SensitiveContentCipher.class),
            mock(ChatAbuseGuard.class),
            mock(SecurityEventService.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class),
            mock(org.springframework.jdbc.core.JdbcTemplate.class),
            mock(UserCommunicationBlockService.class)
        );
        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        questioner.setAccountType("TEST");
        when(users.findById(1L)).thenReturn(Optional.of(questioner));
        when(users.findWithLockById(1L)).thenReturn(Optional.of(questioner));
        when(users.findById(2L)).thenReturn(Optional.of(answerer));
        when(users.findWithLockById(2L)).thenReturn(Optional.of(answerer));

        assertThrows(
            BusinessException.class,
            () -> service.create(
                1L,
                new InquiryService.CreateCommand(
                    2L, 81L, "我正在装修新房", "android"
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
