package com.shixianwen.invitation;

import com.shixianwen.admin.AdminAuditLogRepository;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.common.BusinessException;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitationCampaignServiceTest {
    @Test
    void eligibleUserBindingWaitsForReviewAndApprovalCreditsWithdrawableReward() {
        InvitationCampaignSettingRepository settings = mock(InvitationCampaignSettingRepository.class);
        UserInvitationRepository invitations = mock(UserInvitationRepository.class);
        UserRepository users = mock(UserRepository.class);
        CertificationRepository certifications = mock(CertificationRepository.class);
        WalletService wallet = mock(WalletService.class);
        NotificationService notifications = mock(NotificationService.class);
        AtomicReference<UserInvitation> saved = new AtomicReference<>();
        User invitee = user(8L, "7996702");
        User inviter = user(3L, "7610712");

        when(settings.findById(1L)).thenReturn(Optional.of(setting(true, "6.50")));
        when(users.findWithLockById(8L)).thenReturn(Optional.of(invitee));
        when(users.findByUidAndAccountStatus("7610712", "ACTIVE")).thenReturn(Optional.of(inviter));
        when(certifications.existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            any(), any(), any()
        )).thenReturn(true);
        when(invitations.findByInviteeId(8L))
            .thenAnswer(invocation -> Optional.ofNullable(saved.get()));
        when(invitations.save(any(UserInvitation.class))).thenAnswer(invocation -> {
            UserInvitation item = invocation.getArgument(0);
            item.setId(21L);
            saved.set(item);
            return item;
        });
        when(invitations.findWithLockById(21L)).thenAnswer(invocation -> Optional.of(saved.get()));
        when(invitations.saveAndFlush(any(UserInvitation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        InvitationCampaignService service = service(
            settings, invitations, users, certifications, wallet, notifications
        );
        InvitationCampaignService.UserStatusView result = service.bind(
            invitee,
            "7610712",
            "张三"
        );

        assertTrue(result.submitted());
        assertEquals("7610712", result.inviterUid());
        assertEquals("PENDING", result.status());
        assertEquals(new BigDecimal("6.50"), result.rewardAmount());
        verify(wallet, never()).creditInvitationReward(any(), any(), any());

        service.review(mock(com.shixianwen.admin.AdminUser.class), 21L, true, null, "127.0.0.1");

        verify(wallet).creditInvitationReward(3L, new BigDecimal("6.50"), 21L);
        verify(notifications).send(
            inviter,
            "邀请红包已到账",
            "您成功邀请一位答主，6.5元红包已计入可提现收入。",
            "/profile/wallet"
        );
    }

    @Test
    void userWithoutBothCertificationsCannotBindCode() {
        InvitationCampaignSettingRepository settings = mock(InvitationCampaignSettingRepository.class);
        UserInvitationRepository invitations = mock(UserInvitationRepository.class);
        UserRepository users = mock(UserRepository.class);
        CertificationRepository certifications = mock(CertificationRepository.class);
        WalletService wallet = mock(WalletService.class);
        User invitee = user(8L, "7996702");

        when(settings.findById(1L)).thenReturn(Optional.of(setting(true, "3.00")));
        when(users.findWithLockById(8L)).thenReturn(Optional.of(invitee));
        when(invitations.findByInviteeId(8L)).thenReturn(Optional.empty());
        when(certifications.existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            8L, "IDENTITY", "APPROVED"
        )).thenReturn(true);
        when(certifications.existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            8L, "MAIN_JOB", "APPROVED"
        )).thenReturn(false);

        InvitationCampaignService service = service(
            settings,
            invitations,
            users,
            certifications,
            wallet,
            mock(NotificationService.class)
        );

        BusinessException error = assertThrows(
            BusinessException.class,
            () -> service.bind(invitee, "7610712", "张三")
        );
        assertEquals("完成实名认证和岗位认证后才可以填写邀请码", error.getMessage());
        verify(wallet, never()).creditInvitationReward(any(), any(), any());
    }

    @Test
    void runningCampaignCannotChangeRewardAmount() {
        InvitationCampaignSettingRepository settings = mock(InvitationCampaignSettingRepository.class);
        InvitationCampaignSetting running = setting(true, "3.00");
        when(settings.findById(1L)).thenReturn(Optional.of(running));
        InvitationCampaignService service = service(
            settings,
            mock(UserInvitationRepository.class),
            mock(UserRepository.class),
            mock(CertificationRepository.class),
            mock(WalletService.class),
            mock(NotificationService.class)
        );

        BusinessException error = assertThrows(
            BusinessException.class,
            () -> service.update(
                mock(com.shixianwen.admin.AdminUser.class),
                true,
                new BigDecimal("5.00"),
                "127.0.0.1"
            )
        );

        assertEquals("活动进行中不可调整红包金额，请先下架活动", error.getMessage());
        verify(settings, never()).save(any());
    }

    private InvitationCampaignService service(
        InvitationCampaignSettingRepository settings,
        UserInvitationRepository invitations,
        UserRepository users,
        CertificationRepository certifications,
        WalletService wallet,
        NotificationService notifications
    ) {
        return new InvitationCampaignService(
            settings,
            invitations,
            users,
            certifications,
            wallet,
            notifications,
            mock(AdminAuditLogRepository.class),
            mock(FileStorage.class)
        );
    }

    private InvitationCampaignSetting setting(boolean enabled, String reward) {
        InvitationCampaignSetting setting = new InvitationCampaignSetting();
        setting.setId(1L);
        setting.setEnabled(enabled);
        setting.setRewardAmount(new BigDecimal(reward));
        return setting;
    }

    private User user(Long id, String uid) {
        User user = new User();
        user.setId(id);
        user.setUid(uid);
        user.setAccountStatus("ACTIVE");
        return user;
    }
}
