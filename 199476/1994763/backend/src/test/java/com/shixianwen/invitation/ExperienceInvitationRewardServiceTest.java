package com.shixianwen.invitation;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.common.BusinessException;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExperienceInvitationRewardServiceTest {
    @Test
    void approvedPublicAndMonetizedExperiencesPaySevenYuanOnce() {
        Fixture fixture = fixture();
        when(fixture.certifications.findFirstByUserIdAndCategoryAndExperienceBusinessTypeAndStatusAndEnabledTrueOrderByIdAsc(
            2L, "EXPERIENCE", "PUBLIC_WELFARE", "APPROVED"
        )).thenReturn(Optional.of(certification(20L, fixture.invited)));
        when(fixture.certifications.findFirstByUserIdAndCategoryAndExperienceBusinessTypeAndStatusAndEnabledTrueOrderByIdAsc(
            2L, "EXPERIENCE", "MONETIZED", "APPROVED"
        )).thenReturn(Optional.of(certification(21L, fixture.invited)));

        ExperienceInvitationRewardService.RewardView result = fixture.service.redeem(
            fixture.claimant, "7654321", "13900000002", "127.0.0.1", "device"
        );

        assertEquals(new BigDecimal("2.00"), result.publicWelfareRewardAmount());
        assertEquals(new BigDecimal("5.00"), result.monetizedRewardAmount());
        assertEquals(new BigDecimal("7.00"), result.totalRewardAmount());
        verify(fixture.wallet).creditInvitationReward(1L, new BigDecimal("7.00"), 90L);
    }

    @Test
    void aPreviouslyRedeemedUidCannotPayAgain() {
        Fixture fixture = fixture();
        when(fixture.rewards.findByInvitedUserId(2L))
            .thenReturn(Optional.of(new ExperienceInvitationReward()));

        BusinessException error = assertThrows(
            BusinessException.class,
            () -> fixture.service.redeem(
                fixture.claimant, "7654321", "13900000002", "127.0.0.1", "device"
            )
        );

        assertEquals("该UID的邀请奖金已经领取过", error.getMessage());
        verify(fixture.wallet, never()).creditInvitationReward(any(), any(), any());
    }

    @Test
    void uidWithoutApprovedExperienceCannotPayReward() {
        Fixture fixture = fixture();

        BusinessException error = assertThrows(
            BusinessException.class,
            () -> fixture.service.redeem(
                fixture.claimant, "7654321", "13900000002", "127.0.0.1", "device"
            )
        );

        assertEquals("对方还没有审核通过的经历", error.getMessage());
        verify(fixture.rewards, never()).saveAndFlush(any());
        verify(fixture.wallet, never()).creditInvitationReward(any(), any(), any());
    }

    @Test
    void testAndNormalAccountsCannotShareRealRewardChain() {
        Fixture fixture = fixture();
        fixture.invited.setAccountType("TEST");

        BusinessException error = assertThrows(
            BusinessException.class,
            () -> fixture.service.redeem(
                fixture.claimant, "7654321", "13900000002", "127.0.0.1", "device"
            )
        );

        assertEquals("测试账号与普通账号不能互相领取邀请奖金", error.getMessage());
        verify(fixture.wallet, never()).creditInvitationReward(any(), any(), any());
    }

    @Test
    void uidMustMatchTheInvitedUsersRegisteredPhone() {
        Fixture fixture = fixture();

        BusinessException error = assertThrows(
            BusinessException.class,
            () -> fixture.service.redeem(
                fixture.claimant, "7654321", "13900000999", "127.0.0.1", "device"
            )
        );

        assertEquals("UID或注册手机号不正确", error.getMessage());
        verify(fixture.wallet, never()).creditInvitationReward(any(), any(), any());
    }

    private Fixture fixture() {
        ExperienceInvitationRewardRepository rewards = mock(ExperienceInvitationRewardRepository.class);
        UserRepository users = mock(UserRepository.class);
        CertificationRepository certifications = mock(CertificationRepository.class);
        WalletService wallet = mock(WalletService.class);
        User claimant = user(1L, "7123456", "NORMAL");
        User invited = user(2L, "7654321", "NORMAL");
        when(users.findByUidAndAccountStatus("7654321", "ACTIVE"))
            .thenReturn(Optional.of(invited));
        when(users.findWithLockById(1L)).thenReturn(Optional.of(claimant));
        when(users.findWithLockById(2L)).thenReturn(Optional.of(invited));
        when(rewards.findByInvitedUserId(2L)).thenReturn(Optional.empty());
        when(rewards.saveAndFlush(any(ExperienceInvitationReward.class)))
            .thenAnswer(invocation -> {
                ExperienceInvitationReward reward = invocation.getArgument(0);
                reward.setId(90L);
                reward.setCreatedAt(LocalDateTime.now());
                return reward;
            });

        ExperienceInvitationRewardService service = new ExperienceInvitationRewardService(
            rewards,
            users,
            certifications,
            wallet,
            mock(NotificationService.class),
            mock(AnalyticsEventService.class),
            mock(SecurityEventService.class)
        );
        return new Fixture(service, rewards, certifications, wallet, claimant, invited);
    }

    private User user(Long id, String uid, String accountType) {
        User user = new User();
        user.setId(id);
        user.setUid(uid);
        user.setPhone(id.equals(1L) ? "13900000001" : "13900000002");
        user.setAccountStatus("ACTIVE");
        user.setAccountType(accountType);
        return user;
    }

    private Certification certification(Long id, User user) {
        Certification certification = new Certification();
        certification.setId(id);
        certification.setUser(user);
        certification.setCategory("EXPERIENCE");
        certification.setStatus("APPROVED");
        certification.setEnabled(true);
        return certification;
    }

    private record Fixture(
        ExperienceInvitationRewardService service,
        ExperienceInvitationRewardRepository rewards,
        CertificationRepository certifications,
        WalletService wallet,
        User claimant,
        User invited
    ) {
    }
}
