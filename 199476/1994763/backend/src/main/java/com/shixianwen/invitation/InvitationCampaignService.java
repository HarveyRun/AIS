package com.shixianwen.invitation;

import com.shixianwen.admin.AdminAuditLog;
import com.shixianwen.admin.AdminAuditLogRepository;
import com.shixianwen.admin.AdminUser;
import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationMaterial;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.common.BusinessException;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.MoneyAmounts;
import com.shixianwen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvitationCampaignService {
    private static final long SETTING_ID = 1L;
    private static final String APPROVED = "APPROVED";
    private static final String PENDING = "PENDING";
    private static final String REJECTED = "REJECTED";

    private final InvitationCampaignSettingRepository settings;
    private final UserInvitationRepository invitations;
    private final UserRepository users;
    private final CertificationRepository certifications;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final AdminAuditLogRepository auditLogs;
    private final FileStorage fileStorage;

    @Transactional(readOnly = true)
    public UserStatusView userStatus(User user) {
        InvitationCampaignSetting setting = setting();
        UserInvitation invitation = invitations.findByInviteeId(user.getId()).orElse(null);
        return new UserStatusView(
            setting.isEnabled(),
            user.getUid(),
            eligible(user.getId()),
            invitation != null,
            invitation == null ? null : invitation.getInviter().getUid(),
            invitation == null ? null : invitation.getStatus(),
            MoneyAmounts.normalize(
                invitation == null ? setting.getRewardAmount() : invitation.getRewardAmount()
            )
        );
    }

    @Transactional
    public UserStatusView bind(User currentUser, String invitationCode, String inviterRealName) {
        InvitationCampaignSetting setting = setting();
        if (!setting.isEnabled()) {
            throw BusinessException.badRequest("邀请活动暂未开放");
        }

        User invitee = users.findWithLockById(currentUser.getId())
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        if (invitations.findByInviteeId(invitee.getId()).isPresent()) {
            throw BusinessException.badRequest("邀请码已填写，确认后无法更改");
        }
        if (!eligible(invitee.getId())) {
            throw BusinessException.badRequest("完成实名认证和岗位认证后才可以填写邀请码");
        }

        String code = normalizeCode(invitationCode);
        User inviter = users.findByUidAndAccountStatus(code, "ACTIVE")
            .orElseThrow(() -> BusinessException.badRequest("邀请码不存在"));
        if (inviter.getId().equals(invitee.getId())) {
            throw BusinessException.badRequest("不能填写自己的邀请码");
        }
        if (!eligible(inviter.getId())) {
            throw BusinessException.badRequest("对方尚未完成实名认证和岗位认证");
        }

        BigDecimal reward = MoneyAmounts.requirePositive(setting.getRewardAmount());
        UserInvitation invitation = new UserInvitation();
        invitation.setInviter(inviter);
        invitation.setInvitee(invitee);
        invitation.setInvitationCode(code);
        invitation.setInviterRealName(normalizeRealName(inviterRealName));
        invitation.setRewardAmount(reward);
        invitation.setStatus(PENDING);
        invitations.save(invitation);
        return userStatus(invitee);
    }

    @Transactional(readOnly = true)
    public AdminView adminView() {
        InvitationCampaignSetting setting = setting();
        long successfulInvitations = invitations.countByStatus(APPROVED);
        BigDecimal totalRewards = MoneyAmounts.normalize(invitations.sumRewardAmountByStatus(APPROVED));
        return AdminView.from(setting, successfulInvitations, totalRewards);
    }

    @Transactional(readOnly = true)
    public ReviewPage reviewPage(String keyword, String status, int page, int size) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        String safeStatus = normalizeReviewStatus(status, true);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<UserInvitation> result = invitations.search(
            safeKeyword,
            safeStatus,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("id")))
        );
        return new ReviewPage(
            result.getContent().stream().map(ReviewItem::from).toList(),
            result.getTotalElements(),
            safePage,
            safeSize
        );
    }

    @Transactional(readOnly = true)
    public List<MaterialView> inviterIdentityMaterials(Long invitationId) {
        UserInvitation invitation = invitations.findById(invitationId)
            .orElseThrow(() -> BusinessException.notFound("邀请记录不存在"));
        Certification identity = certifications
            .findFirstByUserIdAndCertificationTypeAndStatusAndEnabledTrueOrderByIdDesc(
                invitation.getInviter().getId(), "IDENTITY", APPROVED
            )
            .orElseThrow(() -> BusinessException.badRequest("对方实名认证已失效"));
        return identity.getMaterials().stream()
            .filter(material -> material.getDeletedAt() == null)
            .map(this::materialView)
            .toList();
    }

    @Transactional(readOnly = true)
    public MaterialView inviteeHandheldIdentityMaterial(Long invitationId) {
        UserInvitation invitation = invitations.findById(invitationId)
            .orElseThrow(() -> BusinessException.notFound("邀请记录不存在"));
        Certification identity = certifications
            .findFirstByUserIdAndCertificationTypeAndStatusAndEnabledTrueOrderByIdDesc(
                invitation.getInvitee().getId(), "IDENTITY", APPROVED
            )
            .orElseThrow(() -> BusinessException.badRequest("受邀人的实名认证已失效"));
        List<CertificationMaterial> activeMaterials = identity.getMaterials().stream()
            .filter(material -> material.getDeletedAt() == null)
            .toList();
        if (activeMaterials.size() < 3) {
            throw BusinessException.badRequest("受邀人的手持身份证照片不存在");
        }
        CertificationMaterial handheld = activeMaterials.get(2);
        if (!"IMAGE".equalsIgnoreCase(handheld.getMaterialKind())) {
            throw BusinessException.badRequest("受邀人的手持身份证照片无法预览");
        }
        return materialView(handheld);
    }

    @Transactional
    public ReviewItem review(
        AdminUser admin,
        Long invitationId,
        boolean approved,
        String reason,
        String ipAddress
    ) {
        UserInvitation invitation = invitations.findWithLockById(invitationId)
            .orElseThrow(() -> BusinessException.notFound("邀请记录不存在"));
        if (!PENDING.equals(invitation.getStatus())) {
            throw BusinessException.badRequest("该邀请已审核");
        }
        String reviewReason = optionalReason(reason);
        if (!approved && reviewReason == null) {
            throw BusinessException.badRequest("驳回时请填写原因");
        }
        if (approved && (!eligible(invitation.getInvitee().getId())
            || !eligible(invitation.getInviter().getId()))) {
            throw BusinessException.badRequest("双方的实名或岗位认证已失效，无法通过审核");
        }

        invitation.setStatus(approved ? APPROVED : REJECTED);
        invitation.setReviewReason(approved ? null : reviewReason);
        invitation.setReviewedByAdmin(admin);
        invitation.setReviewedAt(LocalDateTime.now());
        if (approved) invitation.setRewardedAt(LocalDateTime.now());
        invitation = invitations.saveAndFlush(invitation);

        if (approved) {
            walletService.creditInvitationReward(
                invitation.getInviter().getId(),
                invitation.getRewardAmount(),
                invitation.getId()
            );
            notificationService.send(
                invitation.getInviter(),
                "邀请红包已到账",
                "您成功邀请一位答主，"
                    + invitation.getRewardAmount().stripTrailingZeros().toPlainString()
                    + "元红包已计入可提现收入。",
                "/profile/wallet"
            );
            notificationService.send(
                invitation.getInvitee(),
                "邀请码已通过",
                "邀请关系已确认，对方的红包已发放。",
                ""
            );
        } else {
            notificationService.send(
                invitation.getInvitee(),
                "邀请码未通过",
                reviewReason,
                ""
            );
        }

        auditReview(admin, invitation, approved, reviewReason, ipAddress);
        return ReviewItem.from(invitation);
    }

    @Transactional
    public AdminView update(
        AdminUser admin,
        boolean enabled,
        BigDecimal rewardAmount,
        String ipAddress
    ) {
        InvitationCampaignSetting setting = setting();
        BigDecimal reward = MoneyAmounts.requireExactPositive(rewardAmount, "红包金额");
        if (reward.compareTo(new BigDecimal("999.00")) > 0) {
            throw BusinessException.badRequest("红包金额不能超过999元");
        }
        if (setting.isEnabled() && setting.getRewardAmount().compareTo(reward) != 0) {
            throw BusinessException.badRequest("活动进行中不可调整红包金额，请先下架活动");
        }
        setting.setEnabled(enabled);
        setting.setRewardAmount(reward);
        setting.setUpdatedByAdmin(admin);
        settings.save(setting);

        AdminAuditLog audit = new AdminAuditLog();
        audit.setAdminUser(admin);
        audit.setAction(enabled ? "ENABLE_INVITATION_CAMPAIGN" : "DISABLE_INVITATION_CAMPAIGN");
        audit.setTargetType("INVITATION_CAMPAIGN");
        audit.setTargetId(String.valueOf(SETTING_ID));
        audit.setDetail(
            (enabled ? "上架" : "下架")
                + "邀请答主活动，红包金额="
                + reward.stripTrailingZeros().toPlainString()
                + "元"
        );
        audit.setIpAddress(ipAddress);
        auditLogs.save(audit);
        return adminView();
    }

    private boolean eligible(Long userId) {
        return certifications.existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            userId, "IDENTITY", APPROVED
        ) && certifications.existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            userId, "MAIN_JOB", APPROVED
        );
    }

    private InvitationCampaignSetting setting() {
        return settings.findById(SETTING_ID)
            .orElseThrow(() -> BusinessException.serviceUnavailable("邀请活动配置不存在"));
    }

    private String normalizeCode(String invitationCode) {
        String code = invitationCode == null ? "" : invitationCode.trim();
        if (!code.matches("\\d{7}")) {
            throw BusinessException.badRequest("请输入对方的7位UID");
        }
        return code;
    }

    private String normalizeRealName(String realName) {
        String value = realName == null ? "" : realName.trim();
        if (!value.matches("[\\p{L}·•.\\- '\\u2019]{2,30}")) {
            throw BusinessException.badRequest("请输入对方的真实姓名");
        }
        return value;
    }

    private String normalizeReviewStatus(String status, boolean allowEmpty) {
        String value = status == null ? "" : status.trim().toUpperCase();
        if (allowEmpty && value.isEmpty()) return "";
        if (!List.of(PENDING, APPROVED, REJECTED).contains(value)) {
            throw BusinessException.badRequest("审核状态不正确");
        }
        return value;
    }

    private String optionalReason(String reason) {
        String value = reason == null ? "" : reason.trim();
        if (value.isEmpty()) return null;
        if (value.length() > 300) throw BusinessException.badRequest("审核说明最多300个字");
        return value;
    }

    private MaterialView materialView(CertificationMaterial material) {
        return new MaterialView(
            material.getId(),
            material.getMaterialKind(),
            material.getOriginalName(),
            fileStorage.accessUrl(material.getStorageKey(), StorageVisibility.PRIVATE),
            material.getContentType(),
            material.getFileSize()
        );
    }

    private void auditReview(
        AdminUser admin,
        UserInvitation invitation,
        boolean approved,
        String reason,
        String ipAddress
    ) {
        AdminAuditLog audit = new AdminAuditLog();
        audit.setAdminUser(admin);
        audit.setAction(approved ? "APPROVE_USER_INVITATION" : "REJECT_USER_INVITATION");
        audit.setTargetType("USER_INVITATION");
        audit.setTargetId(String.valueOf(invitation.getId()));
        audit.setDetail(approved ? "通过" : "驳回：" + reason);
        audit.setIpAddress(ipAddress);
        auditLogs.save(audit);
    }

    public record UserStatusView(
        boolean active,
        String ownInvitationCode,
        boolean eligible,
        boolean submitted,
        String inviterUid,
        String status,
        BigDecimal rewardAmount
    ) {
    }

    public record AdminView(
        boolean enabled,
        BigDecimal rewardAmount,
        long successfulInvitations,
        BigDecimal totalRewards,
        String updatedBy,
        LocalDateTime updatedAt
    ) {
        static AdminView from(
            InvitationCampaignSetting setting,
            long successfulInvitations,
            BigDecimal totalRewards
        ) {
            AdminUser admin = setting.getUpdatedByAdmin();
            return new AdminView(
                setting.isEnabled(),
                MoneyAmounts.normalize(setting.getRewardAmount()),
                successfulInvitations,
                totalRewards,
                admin == null ? null : admin.getDisplayName(),
                setting.getUpdatedAt()
            );
        }
    }

    public record ReviewPage(List<ReviewItem> items, long total, int page, int size) {
    }

    public record ReviewItem(
        Long id,
        String inviterUid,
        String inviterPhone,
        String inviterRealName,
        String inviteeUid,
        String inviteeNickname,
        String inviteePhone,
        BigDecimal rewardAmount,
        String status,
        String reviewReason,
        String reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
    ) {
        static ReviewItem from(UserInvitation invitation) {
            AdminUser admin = invitation.getReviewedByAdmin();
            return new ReviewItem(
                invitation.getId(),
                invitation.getInviter().getUid(),
                invitation.getInviter().getPhone(),
                invitation.getInviterRealName(),
                invitation.getInvitee().getUid(),
                invitation.getInvitee().getNickname(),
                invitation.getInvitee().getPhone(),
                MoneyAmounts.normalize(invitation.getRewardAmount()),
                invitation.getStatus(),
                invitation.getReviewReason(),
                admin == null ? null : admin.getDisplayName(),
                invitation.getReviewedAt(),
                invitation.getCreatedAt()
            );
        }
    }

    public record MaterialView(
        Long id,
        String kind,
        String name,
        String url,
        String contentType,
        long size
    ) {
    }
}
