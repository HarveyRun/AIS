package com.shixianwen.promotion;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.banner.HomeBannerService;
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
    void firstApprovedExperiencePaysThreeYuan() {
        Fixture fixture = fixture();

        fixture.service.reward(12L);

        verify(fixture.wallet).creditFirstExperienceReward(
            7L,
            new BigDecimal("3.00"),
            new BigDecimal("0.050000"),
            80L
        );
    }

    @Test
    void userCannotReceiveTheFirstPublishRewardTwice() {
        Fixture fixture = fixture();
        when(fixture.rewards.existsByUserId(7L)).thenReturn(true);

        fixture.service.reward(12L);

        verify(fixture.rewards, never()).saveAndFlush(any());
        verify(fixture.wallet, never()).creditFirstExperienceReward(any(), any(), any(), any());
    }

    @Test
    void existingApprovedExperienceMakesAUserIneligibleForTheCampaign() {
        Fixture fixture = fixture();
        when(fixture.certifications.existsByUserIdAndCategoryAndStatusAndIdNot(
            7L,
            "EXPERIENCE",
            "APPROVED",
            12L
        )).thenReturn(true);

        fixture.service.reward(12L);

        verify(fixture.rewards, never()).saveAndFlush(any());
        verify(fixture.wallet, never()).creditFirstExperienceReward(any(), any(), any(), any());
    }

    private Fixture fixture() {
        FirstExperienceRewardRepository rewards = mock(FirstExperienceRewardRepository.class);
        CertificationRepository certifications = mock(CertificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        HomeBannerService homeBanners = mock(HomeBannerService.class);
        User user = new User();
        user.setId(7L);
        user.setAccountStatus("ACTIVE");
        Certification certification = new Certification();
        certification.setId(12L);
        certification.setUser(user);
        certification.setCategory("EXPERIENCE");
        certification.setStatus("APPROVED");
        certification.setEnabled(true);
        when(certifications.findById(12L)).thenReturn(Optional.of(certification));
        when(users.findWithLockById(7L)).thenReturn(Optional.of(user));
        when(rewards.existsByUserId(7L)).thenReturn(false);
        when(homeBanners.isActionAvailable("FIRST_EXPERIENCE_REWARD"))
            .thenReturn(true);
        when(wallet.quoteIncomeServiceFee(new BigDecimal("3.00"), "ANDROID"))
            .thenReturn(new com.shixianwen.wallet.PlatformServiceFeePolicy.SettlementQuote(
                "ANDROID",
                new BigDecimal("3.00"),
                new BigDecimal("0.050000"),
                new BigDecimal("0.15"),
                new BigDecimal("2.85")
            ));
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
            mock(AnalyticsEventService.class),
            homeBanners,
            mock(org.springframework.jdbc.core.JdbcTemplate.class),
            mock(com.shixianwen.security.SecurityEventService.class)
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
