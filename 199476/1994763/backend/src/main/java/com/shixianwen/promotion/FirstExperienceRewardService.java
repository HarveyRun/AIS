package com.shixianwen.promotion;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FirstExperienceRewardService {
    private static final String PUBLIC_WELFARE = "PUBLIC_WELFARE";
    private static final String MONETIZED = "MONETIZED";
    private static final BigDecimal PUBLIC_WELFARE_REWARD = new BigDecimal("2.00");
    private static final BigDecimal MONETIZED_REWARD = new BigDecimal("5.00");

    private final FirstExperienceRewardRepository rewards;
    private final CertificationRepository certifications;
    private final UserRepository users;
    private final WalletService wallet;
    private final NotificationService notifications;
    private final AnalyticsEventService analytics;

    @Transactional
    public void reward(Long certificationId) {
        Certification certification = certifications.findById(certificationId).orElse(null);
        if (!eligible(certification)) return;

        User user = users.findWithLockById(certification.getUser().getId()).orElse(null);
        if (user == null || !"ACTIVE".equals(user.getAccountStatus())) return;
        if (rewards.existsByUserId(user.getId())) return;
        if (certifications.existsByUserIdAndCategoryAndStatusAndIdNot(
            user.getId(),
            "EXPERIENCE",
            "APPROVED",
            certification.getId()
        )) {
            return;
        }

        String businessType = normalizeBusinessType(certification.getExperienceBusinessType());
        BigDecimal amount = PUBLIC_WELFARE.equals(businessType)
            ? PUBLIC_WELFARE_REWARD
            : MONETIZED_REWARD;

        FirstExperienceReward reward = new FirstExperienceReward();
        reward.setUser(user);
        reward.setCertification(certification);
        reward.setExperienceBusinessType(businessType);
        reward.setAmount(amount);
        reward = rewards.saveAndFlush(reward);

        wallet.creditFirstExperienceReward(user.getId(), amount, reward.getId());
        notifications.send(
            user,
            "首次发布奖励已到账",
            "您的首段经历已成功发布，" + amount.stripTrailingZeros().toPlainString()
                + "元奖励已计入可提现收入。",
            "/profile/wallet"
        );
        analytics.recordBusinessAfterCommit(
            user,
            "first_experience_rewarded",
            analytics.properties(
                "reward_id", reward.getId(),
                "certification_id", certification.getId(),
                "business_type", businessType,
                "amount", amount.toPlainString()
            )
        );
    }

    private boolean eligible(Certification certification) {
        return certification != null
            && "EXPERIENCE".equals(certification.getCategory())
            && "APPROVED".equals(certification.getStatus())
            && certification.isEnabled();
    }

    private String normalizeBusinessType(String value) {
        return PUBLIC_WELFARE.equals(value) ? PUBLIC_WELFARE : MONETIZED;
    }
}
