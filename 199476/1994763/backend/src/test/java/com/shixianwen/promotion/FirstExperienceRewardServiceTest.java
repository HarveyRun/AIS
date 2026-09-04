package com.shixianwen.promotion;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirstExperienceRewardServiceTest {
    @Test
    void firstApprovedPublicExperiencePaysTwoYuan() {
        Fixture fixture = fixture("PUBLIC_WELFARE");

        fixture.service.reward(12L);

        verify(fixture.wallet).creditFirstExperienceReward(
            7L,
            new BigDecimal("2.00"),
            80L
        );
    }

    @Test
    void firstApprovedMonetizedExperiencePaysFiveYuan() {
        Fixture fixture = fixture("MONETIZED");

        fixture.service.reward(12L);

        verify(fixture.wallet).creditFirstExperienceReward(
            7L,
            new BigDecimal("5.00"),
            80L
        );
    }

    @Test
    void userCannotReceiveTheFirstPublishRewardTwice() {
        Fixture fixture = fixture("MONETIZED");
        when(fixture.rewards.existsByUserId(7L)).thenReturn(true);

        fixture.service.reward(12L);

        verify(fixture.rewards, never()).saveAndFlush(any());
        verify(fixture.wallet, never()).creditFirstExperienceReward(any(), any(), any());
    }

    @Test
    void existingApprovedExperienceMakesAUserIneligibleForTheCampaign() {
        Fixture fixture = fixture("MONETIZED");
        when(fixture.certifications.existsByUserIdAndCategoryAndStatusAndIdNot(
            7L,
            "EXPERIENCE",
            "APPROVED",
            12L
        )).thenReturn(true);

        fixture.service.reward(12L);

        verify(fixture.rewards, never()).saveAndFlush(any());
        verify(fixture.wallet, never()).creditFirstExperienceReward(any(), any(), any());
    }

    private Fixture fixture(String businessType) {
        FirstExperienceRewardRepository rewards = mock(FirstExperienceRewardRepository.class);
        CertificationRepository certifications = mock(CertificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        User user = new User();
        user.setId(7L);
        user.setAccountStatus("ACTIVE");
        Certification certification = new Certification();
        certification.setId(12L);
        certification.setUser(user);
        certification.setCategory("EXPERIENCE");
        certification.setExperienceBusinessType(businessType);
        certification.setStatus("APPROVED");
        certification.setEnabled(true);
        when(certifications.findById(12L)).thenReturn(Optional.of(certification));
        when(users.findWithLockById(7L)).thenReturn(Optional.of(user));
        when(rewards.existsByUserId(7L)).thenReturn(false);
        when(rewards.saveAndFlush(any(FirstExperienceReward.class)))
            .thenAnswer(invocation -> {
                FirstExperienceReward reward = invocation.getArgument(0);
                reward.setId(80L);
                return reward;
            });
        FirstExperienceRewardService service = new FirstExperienceRewardService(
            rewards,
            certifications,
            users,
            wallet,
            mock(NotificationService.class),
            mock(AnalyticsEventService.class)
        );
        return new Fixture(service, rewards, certifications, wallet);
    }

    private record Fixture(
        FirstExperienceRewardService service,
        FirstExperienceRewardRepository rewards,
        CertificationRepository certifications,
        WalletService wallet
    ) {
    }
}
