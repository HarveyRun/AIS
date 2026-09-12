package com.shixianwen.promotion;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.banner.HomeBannerService;
import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.config.AppGlobalSettingService;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletService;
import com.shixianwen.security.SecurityEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FirstExperienceRewardService {
    private static final BigDecimal FIRST_EXPERIENCE_REWARD = new BigDecimal("3.00");

    private final FirstExperienceRewardRepository rewards;
    private final CertificationRepository certifications;
    private final UserRepository users;
    private final WalletService wallet;
    private final NotificationService notifications;
    private final AnalyticsEventService analytics;
    private final HomeBannerService homeBanners;
    private final JdbcTemplate jdbc;
    private final SecurityEventService securityEvents;
    @Autowired(required = false)
    private AppGlobalSettingService globalSettings;

    @Transactional
    public void reward(Long certificationId) {
        AppGlobalSettingService.Settings settings = globalSettings == null ? null : globalSettings.current();
        if (settings == null) {
            if (!homeBanners.isActionAvailable("FIRST_EXPERIENCE_REWARD")) return;
        } else if (!globalSettings.firstExperienceRewardActive(settings, LocalDateTime.now())) {
            return;
        }
        BigDecimal rewardAmount = settings == null ? FIRST_EXPERIENCE_REWARD : settings.firstExperienceRewardAmount();
        Certification certification = certifications.findById(certificationId).orElse(null);
        if (!eligible(certification)) return;
        var feeQuote = wallet.quoteIncomeServiceFee(rewardAmount, certification.getSourceClientPlatform());
        BigDecimal feeRate = feeQuote.serviceFeeRate();

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

        FirstExperienceReward reward = new FirstExperienceReward();
        reward.setUser(user);
        reward.setCertification(certification);
        reward.setAmount(rewardAmount);
        BigDecimal feeAmount = feeQuote.serviceFeeAmount();
        BigDecimal incomeAmount = feeQuote.answererIncomeAmount();
        reward.setFeeRate(feeRate);
        reward.setFeeAmount(feeAmount);
        reward.setUserIncomeAmount(incomeAmount);
        List<String> riskReasons = rewardRiskReasons(user, certification);
        reward.setRiskLevel(riskReasons.isEmpty() ? "LOW" : "HIGH");
        reward.setRiskReasons(riskReasons.isEmpty() ? null : String.join("；", riskReasons));
        reward = rewards.saveAndFlush(reward);

        if (!riskReasons.isEmpty()) {
            securityEvents.recordSafely(
                user.getId(), null, "FIRST_EXPERIENCE_REWARD_RISK", "HIGH",
                user.getLastLoginIp(), user.getLastLoginDeviceId(),
                "rewardId=" + reward.getId() + ", reasons=" + String.join("|", riskReasons)
            );
        }

        wallet.creditFirstExperienceReward(user.getId(), rewardAmount, feeRate, reward.getId());
        notifications.send(
            user,
            "首次发布奖励已到账",
            "您的首段经历已成功发布，扣除手续费后"
                + incomeAmount.stripTrailingZeros().toPlainString() + "元已计入可提现收入。",
            "/profile/wallet"
        );
        analytics.recordBusinessAfterCommit(
            user,
            "first_experience_rewarded",
            analytics.properties(
                "reward_id", reward.getId(),
                "certification_id", certification.getId(),
                "amount", rewardAmount.toPlainString(),
                "fee_amount", feeAmount.toPlainString(),
                "income_amount", incomeAmount.toPlainString()
            )
        );
    }

    private boolean eligible(Certification certification) {
        return certification != null
            && "EXPERIENCE".equals(certification.getCategory())
            && "APPROVED".equals(certification.getStatus())
            && certification.isEnabled();
    }

    private List<String> rewardRiskReasons(User user, Certification certification) {
        List<String> reasons = new ArrayList<>();
        if (meaningful(user.getRegisterDeviceId())) {
            Long sameDevice = jdbc.queryForObject(
                "SELECT COUNT(*) FROM first_experience_rewards r JOIN users u ON u.id=r.user_id " +
                    "WHERE r.user_id<>? AND (u.register_device_id=? OR u.last_login_device_id=?)",
                Long.class, user.getId(), user.getRegisterDeviceId(), user.getRegisterDeviceId()
            );
            if (sameDevice != null && sameDevice > 0) reasons.add("同一设备已有其他奖励账户");
        }
        if (meaningfulIp(user.getRegisterIp())) {
            Long sameIp = jdbc.queryForObject(
                "SELECT COUNT(*) FROM first_experience_rewards r JOIN users u ON u.id=r.user_id " +
                    "WHERE r.user_id<>? AND (u.register_ip=? OR u.last_login_ip=?)",
                Long.class, user.getId(), user.getRegisterIp(), user.getRegisterIp()
            );
            if (sameIp != null && sameIp > 0) reasons.add("同一IP已有其他奖励账户");
        }
        String current = normalize(certification.getTitle() + " " + certification.getDescription());
        if (!current.isBlank()) {
            List<String> previous = jdbc.queryForList(
                "SELECT CONCAT(COALESCE(c.title,''),' ',COALESCE(c.description,'')) " +
                    "FROM first_experience_rewards r JOIN certifications c ON c.id=r.certification_id " +
                    "WHERE r.user_id<>? ORDER BY r.id DESC LIMIT 1000",
                String.class,
                user.getId()
            );
            boolean similar = previous.stream().map(this::normalize)
                .anyMatch(value -> similarity(current, value) >= 0.82d);
            if (similar) reasons.add("经历内容与其他奖励内容高度相似");
        }
        return reasons;
    }

    private boolean meaningful(String value) {
        return value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value);
    }

    private boolean meaningfulIp(String value) {
        return meaningful(value) && !value.startsWith("127.") && !"::1".equals(value);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private double similarity(String left, String right) {
        if (left.isBlank() || right.isBlank()) return 0d;
        if (left.equals(right)) return 1d;
        Set<String> a = grams(left);
        Set<String> b = grams(right);
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0d : (double) intersection.size() / union.size();
    }

    private Set<String> grams(String value) {
        Set<String> result = new HashSet<>();
        if (value.length() < 2) {
            result.add(value);
            return result;
        }
        for (int index = 0; index < value.length() - 1; index++) {
            result.add(value.substring(index, index + 2));
        }
        return result;
    }
}
