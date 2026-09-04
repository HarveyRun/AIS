package com.shixianwen.invitation;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.auth.AccountPenaltyException;
import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.common.BusinessException;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.MoneyAmounts;
import com.shixianwen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExperienceInvitationRewardService {
    private static final String APPROVED = "APPROVED";
    private static final String EXPERIENCE = "EXPERIENCE";
    private static final String PUBLIC_WELFARE = "PUBLIC_WELFARE";
    private static final String MONETIZED = "MONETIZED";
    private static final BigDecimal PUBLIC_WELFARE_REWARD = new BigDecimal("2.00");
    private static final BigDecimal MONETIZED_REWARD = new BigDecimal("5.00");

    private final ExperienceInvitationRewardRepository rewards;
    private final UserRepository users;
    private final CertificationRepository certifications;
    private final WalletService wallet;
    private final NotificationService notifications;
    private final AnalyticsEventService analytics;
    private final SecurityEventService securityEvents;
    private final InvitationSubmissionGuard submissionGuard;
    private final ExperienceInvitationRelationshipRepository relationships;

    @Transactional
    public RewardView redeem(
        User currentUser,
        String invitedUid,
        String invitedPhone,
        String ipAddress,
        String deviceId
    ) {
        String code = text(invitedUid);
        String phone = text(invitedPhone);
        if (!code.matches("\\d{7}") || !phone.matches("^1[3-9]\\d{9}$")) {
            rejectInvalidIdentity(currentUser, ipAddress, deviceId, code);
        }
        User matched = users.findByUidAndAccountStatus(code, "ACTIVE").orElse(null);
        if (matched == null || !secureEquals(matched.getPhone(), phone)) {
            rejectInvalidIdentity(currentUser, ipAddress, deviceId, code);
        }
        submissionGuard.clear(currentUser.getId());
        if (matched.getId().equals(currentUser.getId())) {
            throw BusinessException.badRequest("不能填写自己的UID");
        }

        Long lowerUserId = Math.min(currentUser.getId(), matched.getId());
        Long higherUserId = Math.max(currentUser.getId(), matched.getId());
        User lowerUser = users.findWithLockById(lowerUserId)
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        User higherUser = users.findWithLockById(higherUserId)
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        User claimant = lowerUser.getId().equals(currentUser.getId()) ? lowerUser : higherUser;
        User invitedUser = lowerUser.getId().equals(matched.getId()) ? lowerUser : higherUser;
        ensureActive(claimant);
        ensureActive(invitedUser);
        if (!sameAccountWorld(claimant, invitedUser)) {
            securityEvents.recordSafely(
                claimant.getId(), null, "INVITATION_REWARD_ACCOUNT_MIX", "HIGH",
                ipAddress, deviceId, "invitedUid=" + code
            );
            throw BusinessException.badRequest("测试账号与普通账号不能互相领取邀请奖金");
        }
        if (relationships.findByLowerUserIdAndHigherUserId(
            lowerUserId,
            higherUserId
        ).isPresent()) {
            throw BusinessException.badRequest("双方已经存在邀请关系，不能互相邀请");
        }
        if (rewards.findByInvitedUserId(invitedUser.getId()).isPresent()) {
            throw BusinessException.badRequest("该UID的邀请奖金已经领取过");
        }

        Certification publicWelfare = certifications
            .findFirstByUserIdAndCategoryAndExperienceBusinessTypeAndStatusAndEnabledTrueOrderByIdAsc(
                invitedUser.getId(), EXPERIENCE, PUBLIC_WELFARE, APPROVED
            )
            .orElse(null);
        Certification monetized = certifications
            .findFirstByUserIdAndCategoryAndExperienceBusinessTypeAndStatusAndEnabledTrueOrderByIdAsc(
                invitedUser.getId(), EXPERIENCE, MONETIZED, APPROVED
            )
            .orElse(null);
        if (publicWelfare == null && monetized == null) {
            throw BusinessException.badRequest("对方还没有审核通过的经历");
        }

        BigDecimal publicAmount = publicWelfare == null ? MoneyAmounts.ZERO : PUBLIC_WELFARE_REWARD;
        BigDecimal monetizedAmount = monetized == null ? MoneyAmounts.ZERO : MONETIZED_REWARD;
        BigDecimal total = MoneyAmounts.add(publicAmount, monetizedAmount);

        ExperienceInvitationReward reward = new ExperienceInvitationReward();
        reward.setClaimant(claimant);
        reward.setInvitedUser(invitedUser);
        reward.setInvitationCode(code);
        reward.setPublicWelfareCertification(publicWelfare);
        reward.setMonetizedCertification(monetized);
        reward.setPublicWelfareRewardAmount(publicAmount);
        reward.setMonetizedRewardAmount(monetizedAmount);
        reward.setTotalRewardAmount(total);
        try {
            reward = rewards.saveAndFlush(reward);
            ExperienceInvitationRelationship relationship =
                new ExperienceInvitationRelationship();
            relationship.setLowerUserId(lowerUserId);
            relationship.setHigherUserId(higherUserId);
            relationships.saveAndFlush(relationship);
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.badRequest("双方已经存在邀请关系或该UID的奖金已被领取");
        }

        wallet.creditInvitationReward(claimant.getId(), total, reward.getId());
        notifications.send(
            claimant,
            "邀请奖金已到账",
            "您邀请的好友已分享审核通过的经历，" + moneyText(total) + "元已计入可提现收入。",
            "/profile/wallet"
        );
        analytics.recordBusinessAfterCommit(claimant, "invitation_rewarded", Map.of(
            "invitation_reward_id", reward.getId(),
            "invited_user_id", invitedUser.getId(),
            "public_welfare_reward", publicAmount.toPlainString(),
            "monetized_reward", monetizedAmount.toPlainString(),
            "total_reward", total.toPlainString()
        ));
        securityEvents.recordSafely(
            claimant.getId(), null, "INVITATION_REWARD_PAID", "MEDIUM",
            ipAddress, deviceId,
            "rewardId=" + reward.getId() + ",invitedUserId=" + invitedUser.getId() + ",amount=" + total
        );
        return RewardView.from(reward);
    }

    private void ensureActive(User user) {
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw BusinessException.badRequest("账号当前不可用");
        }
    }

    private boolean sameAccountWorld(User first, User second) {
        return isTest(first) == isTest(second);
    }

    private boolean isTest(User user) {
        return "TEST".equals(user.getAccountType());
    }

    private void rejectInvalidIdentity(
        User currentUser,
        String ipAddress,
        String deviceId,
        String invitedUid
    ) {
        int attempts = submissionGuard.recordInvalid(
            currentUser.getId(),
            ipAddress,
            deviceId,
            invitedUid
        );
        if (attempts > 8) {
            throw new AccountPenaltyException("违反平台规则", null);
        }
        throw BusinessException.badRequest(
            "UID或注册手机号不正确，已累计" + attempts + "次异常提交"
        );
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean secureEquals(String expected, String actual) {
        return expected != null && MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String moneyText(BigDecimal amount) {
        return MoneyAmounts.normalize(amount).stripTrailingZeros().toPlainString();
    }

    public record RewardView(
        Long id,
        String invitedUid,
        BigDecimal publicWelfareRewardAmount,
        BigDecimal monetizedRewardAmount,
        BigDecimal totalRewardAmount,
        LocalDateTime rewardedAt
    ) {
        static RewardView from(ExperienceInvitationReward reward) {
            return new RewardView(
                reward.getId(),
                reward.getInvitedUser().getUid(),
                MoneyAmounts.normalize(reward.getPublicWelfareRewardAmount()),
                MoneyAmounts.normalize(reward.getMonetizedRewardAmount()),
                MoneyAmounts.normalize(reward.getTotalRewardAmount()),
                reward.getCreatedAt()
            );
        }
    }
}
