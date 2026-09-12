package com.shixianwen.inquiry;

import com.shixianwen.common.BusinessException;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.PlatformServiceFeePolicy;
import com.shixianwen.wallet.WalletService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InquiryAudioAppointmentServiceTest {
    @Test
    void createsBasicCallAndReservesAvailableBalance() {
        Fixture f = fixture();

        var result = f.service.create(1L, 91L);

        assertEquals("BASIC", result.appointmentType());
        assertEquals("CONNECTING", result.status());
        assertEquals(new BigDecimal("100.00"), result.amount());
        assertTrue(result.connectDeadline().isAfter(LocalDateTime.now().plusSeconds(50)));
        verify(f.wallet).reserveMeteredAudio(1L, 301L);
    }

    @Test
    void answererCanAnswerDirectCall() {
        Fixture f = fixture();
        f.service.create(1L, 91L);
        InquiryAudioAppointment call = f.saved.get();
        when(f.appointments.findWithLockById(301L)).thenReturn(Optional.of(call));

        var result = f.service.answer(2L, 91L, 301L);

        assertEquals("CONNECTING", result.status());
        assertTrue(result.acceptedAt() != null);
    }

    @Test
    void answerAfterRingingTimeoutRefundsCall() {
        Fixture f = fixture();
        f.service.create(1L, 91L);
        InquiryAudioAppointment call = f.saved.get();
        when(f.appointments.findWithLockById(301L)).thenReturn(Optional.of(call));
        call.setConnectDeadline(LocalDateTime.now().minusSeconds(1));

        var result = f.service.answer(2L, 91L, 301L);

        assertEquals("CONNECTION_FAILED", result.status());
        verify(f.wallet).refundAudioAppointment(
            1L, new BigDecimal("40.00"), new BigDecimal("60.00"), 301L
        );
    }

    @Test
    void connectedCallKeepsInquiryFrozenAmountConsistentWithReservedSources() {
        Fixture f = fixture();
        f.service.create(1L, 91L);
        InquiryAudioAppointment call = f.saved.get();
        when(f.appointments.findWithLockById(301L)).thenReturn(Optional.of(call));
        f.service.answer(2L, 91L, 301L);

        f.service.connected(1L, 91L, 301L);
        var result = f.service.connected(2L, 91L, 301L);

        assertEquals("ACTIVE", result.status());
        assertEquals(call.getAmount(), call.getInquiry().getAmount());
        assertEquals(
            call.getAmount(),
            call.getInquiry().getFrozenRechargeAmount()
                .add(call.getInquiry().getFrozenIncomeAmount())
        );
    }

    @Test
    void answererCanHangUpAndFiveMinutesAddsQuotaForBoth() {
        Fixture f = fixture();
        f.service.create(1L, 91L);
        InquiryAudioAppointment call = f.saved.get();
        call.setStatus("ACTIVE");
        call.setConnectedAt(LocalDateTime.now().minusMinutes(5).minusSeconds(1));
        call.setActiveSegmentStartedAt(call.getConnectedAt());
        call.getInquiry().setStatus("PAID_ACTIVE");
        when(f.appointments.findWithLockById(301L)).thenReturn(Optional.of(call));

        var result = f.service.finishCall(2L, 91L, 301L);

        assertEquals("COMPLETED", result.status());
        assertEquals("ANSWERER_HANGUP", result.endReason());
        assertEquals(100, call.getInquiry().getQuestionerTextLimit());
        assertEquals(100, call.getInquiry().getAnswererTextLimit());
        verify(f.wallet).settleMeteredAudio(
            any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void shortCallDoesNotAddTextQuota() {
        Fixture f = fixture();
        f.service.create(1L, 91L);
        InquiryAudioAppointment call = f.saved.get();
        call.setStatus("ACTIVE");
        call.setConnectedAt(LocalDateTime.now().minusMinutes(4));
        call.setActiveSegmentStartedAt(call.getConnectedAt());
        call.getInquiry().setStatus("PAID_ACTIVE");
        when(f.appointments.findWithLockById(301L)).thenReturn(Optional.of(call));

        f.service.finishCall(1L, 91L, 301L);

        assertEquals(50, call.getInquiry().getQuestionerTextLimit());
        assertEquals(50, call.getInquiry().getAnswererTextLimit());
    }

    @Test
    void rejectedCallRefundsFullReserve() {
        Fixture f = fixture();
        f.service.create(1L, 91L);
        InquiryAudioAppointment call = f.saved.get();
        when(f.appointments.findWithLockById(301L)).thenReturn(Optional.of(call));

        assertEquals("REJECTED", f.service.reject(2L, 91L, 301L).status());
        verify(f.wallet).refundAudioAppointment(
            1L, new BigDecimal("40.00"), new BigDecimal("60.00"), 301L
        );
    }

    @Test
    void outsiderCannotFinishCall() {
        Fixture f = fixture();
        f.service.create(1L, 91L);
        InquiryAudioAppointment call = f.saved.get();
        call.setStatus("ACTIVE");
        when(f.appointments.findWithLockById(301L)).thenReturn(Optional.of(call));

        assertThrows(BusinessException.class, () -> f.service.finishCall(3L, 91L, 301L));
    }

    private static Fixture fixture() {
        InquiryAudioAppointmentRepository appointments = mock(InquiryAudioAppointmentRepository.class);
        InquiryRepository inquiries = mock(InquiryRepository.class);
        InquiryMessageRepository messages = mock(InquiryMessageRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        NotificationService notifications = mock(NotificationService.class);
        RealtimePublisher realtime = mock(RealtimePublisher.class);
        InquiryAudioAppointmentService service = new InquiryAudioAppointmentService(
            appointments, inquiries, messages, users, wallet, notifications, realtime,
            mock(UserCommunicationBlockService.class), mock(org.springframework.jdbc.core.JdbcTemplate.class)
        );

        User questioner = user(1L, "1000001");
        User answerer = user(2L, "2000002");
        Inquiry inquiry = new Inquiry();
        inquiry.setId(91L);
        inquiry.setQuestioner(questioner);
        inquiry.setAnswerer(answerer);
        inquiry.setStatus("ACTIVE");
        inquiry.setFundsStatus("NONE");
        inquiry.setFlowVersion(2);
        inquiry.setHourlyRateSnapshot(200);
        inquiry.setClientPlatform("ANDROID");
        inquiry.setQuestionerTextLimit(50);
        inquiry.setAnswererTextLimit(50);
        inquiry.setConversationExpiresAt(LocalDateTime.now().plusDays(20));

        when(inquiries.findWithLockById(91L)).thenReturn(Optional.of(inquiry));
        when(realtime.isUserOnline(2L)).thenReturn(true);
        when(users.findWithLockById(1L)).thenReturn(Optional.of(questioner));
        when(users.findWithLockById(2L)).thenReturn(Optional.of(answerer));
        when(wallet.reserveMeteredAudio(1L, 301L)).thenReturn(
            new WalletService.FrozenAllocation(new BigDecimal("40.00"), new BigDecimal("60.00"))
        );
        when(wallet.quoteInquirySettlement(any(BigDecimal.class), any(String.class))).thenAnswer(invocation -> {
            BigDecimal gross = invocation.getArgument(0);
            BigDecimal fee = gross.multiply(new BigDecimal("0.10")).setScale(2);
            return new PlatformServiceFeePolicy.SettlementQuote(
                "ANDROID", gross, new BigDecimal("0.100000"), fee, gross.subtract(fee)
            );
        });
        when(messages.countByInquiryIdAndSenderIdAndCountsTowardFreeLimitTrueAndMessageTypeIn(
            any(), any(), any()
        )).thenReturn(0L);

        AtomicReference<InquiryAudioAppointment> saved = new AtomicReference<>();
        when(appointments.saveAndFlush(any(InquiryAudioAppointment.class))).thenAnswer(invocation -> {
            InquiryAudioAppointment call = invocation.getArgument(0);
            call.setId(301L);
            call.setCreatedAt(LocalDateTime.now());
            saved.set(call);
            return call;
        });
        when(messages.saveAndFlush(any(InquiryMessage.class))).thenAnswer(invocation -> {
            InquiryMessage message = invocation.getArgument(0);
            message.setId(401L);
            message.setCreatedAt(LocalDateTime.now());
            return message;
        });
        return new Fixture(service, appointments, wallet, realtime, saved);
    }

    private static User user(Long id, String uid) {
        User user = new User();
        user.setId(id);
        user.setUid(uid);
        user.setPhone("1380000000" + id);
        return user;
    }

    private record Fixture(
        InquiryAudioAppointmentService service,
        InquiryAudioAppointmentRepository appointments,
        WalletService wallet,
        RealtimePublisher realtime,
        AtomicReference<InquiryAudioAppointment> saved
    ) {}
}
