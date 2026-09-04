package com.shixianwen.invitation;

import com.shixianwen.notification.NotificationService;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.PermanentBanPayoutService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitationSubmissionGuardTest {
    @Test
    void ninthInvalidSubmissionPermanentlyBansTheUser() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UserRepository users = mock(UserRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        RealtimePublisher realtime = mock(RealtimePublisher.class);
        PermanentBanPayoutService payouts = mock(PermanentBanPayoutService.class);
        User user = new User();
        user.setId(7L);
        user.setAccountStatus("ACTIVE");
        when(users.findWithLockById(7L)).thenReturn(Optional.of(user));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(7L))).thenReturn(9);
        InvitationSubmissionGuard guard = new InvitationSubmissionGuard(
            jdbc,
            users,
            notifications,
            realtime,
            payouts,
            mock(SecurityEventService.class)
        );

        int result = guard.recordInvalid(7L, "127.0.0.1", "device", "7000000");

        assertEquals(9, result);
        assertEquals("SUSPENDED", user.getAccountStatus());
        assertEquals(null, user.getBanUntil());
        verify(users).saveAndFlush(user);
        verify(payouts).captureForPermanentBan(7L);
        verify(notifications).send(
            user,
            "账号处罚通知",
            "账号因违反平台规则已被永久封禁",
            "/profile"
        );
        verify(realtime).afterCommit(eq(7L), eq("ACCOUNT_PENALTY"), any());
    }

    @Test
    void validIdentityClearsPreviousInvalidAttempts() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        InvitationSubmissionGuard guard = new InvitationSubmissionGuard(
            jdbc,
            mock(UserRepository.class),
            mock(NotificationService.class),
            mock(RealtimePublisher.class),
            mock(PermanentBanPayoutService.class),
            mock(SecurityEventService.class)
        );

        guard.clear(7L);

        verify(jdbc).update(anyString(), eq(7L));
    }
}
