package com.shixianwen.contribution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.admin.AdminAuditLog;
import com.shixianwen.admin.AdminAuditLogRepository;
import com.shixianwen.admin.AdminUser;
import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.common.BusinessException;
import com.shixianwen.content.SensitiveContentCipher;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.inquiry.ViolationService;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContentContributionService {
    public static final int MAX_TOTAL = 200;
    public static final int MAX_PENDING = 5;
    public static final int MAX_JOBS = 10;
    public static final int MAX_MATTER_NAME_LENGTH = 10;
    private static final String PENDING = "PENDING";
    private static final String ADOPTED = "ADOPTED";
    private static final String REJECTED = "REJECTED";
    private static final String VIOLATION_REJECTED = "VIOLATION_REJECTED";
    private static final Set<String> REVIEW_DECISIONS = Set.of(
        ADOPTED,
        REJECTED,
        VIOLATION_REJECTED
    );

    private final ContentContributionRepository contributions;
    private final ContentContributionJobRepository jobs;
    private final ContentContributionRewardRepository rewards;
    private final UserRepository users;
    private final SensitiveWordService sensitiveWords;
    private final SensitiveContentCipher cipher;
    private final ObjectMapper objectMapper;
    private final WalletService walletService;
    private final NotificationService notifications;
    private final ViolationService violations;
    private final AdminAuditLogRepository auditLogs;
    private final AnalyticsEventService analytics;

    @Transactional(readOnly = true)
    public UserSummary summary(Long userId) {
        long total = contributions.countByUserId(userId);
        long pending = contributions.countByUserIdAndStatus(userId, PENDING);
        long adopted = contributions.countByUserIdAndStatus(userId, ADOPTED);
        BigDecimal earnedReward = rewards.sumRewardAmountByUserId(userId);
        return new UserSummary(
            total,
            MAX_TOTAL,
            pending,
            MAX_PENDING,
            adopted,
            earnedReward,
            total < MAX_TOTAL && pending < MAX_PENDING,
            1,
            3
        );
    }

    @Transactional
    public ContributionView submit(User currentUser, SubmitCommand command) {
        User user = users.findWithLockById(currentUser.getId())
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        long total = contributions.countByUserId(user.getId());
        if (total >= MAX_TOTAL) {
            throw BusinessException.badRequest("您累计最多可以提交200条共建内容");
        }
        long pending = contributions.countByUserIdAndStatus(user.getId(), PENDING);
        if (pending >= MAX_PENDING) {
            throw BusinessException.badRequest("您已有5条内容等待审核，请在审核完成后再提交");
        }

        ValidatedSubmission value = validate(command);
        ContentContribution item = new ContentContribution();
        item.setUser(user);
        item.setMatterName(sensitiveWords.mask(value.matterName()));
        item.setRawPayloadEncrypted(cipher.encrypt(toJson(value)));
        item.setCandidateFingerprint(fingerprint(value.matterName(), value.jobs()));
        item.setStatus(PENDING);
        item = contributions.saveAndFlush(item);

        List<ContentContributionJob> savedJobs = new ArrayList<>();
        for (int index = 0; index < value.jobs().size(); index++) {
            ValidatedJob source = value.jobs().get(index);
            ContentContributionJob job = new ContentContributionJob();
            job.setContribution(item);
            job.setJobName(sensitiveWords.mask(source.jobName()));
            job.setResponsibility(sensitiveWords.mask(source.responsibility()));
            job.setSortOrder(index);
            savedJobs.add(jobs.save(job));
        }
        item.setJobs(savedJobs);
        analytics.recordBusinessAfterCommit(user, "content_contribution_submit", java.util.Map.of(
            "contribution_id", item.getId(),
            "job_count", savedJobs.size()
        ));
        return ContributionView.from(item, savedJobs, false);
    }

    @Transactional(readOnly = true)
    public PageResult userList(Long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<ContentContribution> result = contributions.findByUserIdOrderByCreatedAtDescIdDesc(
            userId,
            PageRequest.of(safePage, safeSize)
        );
        return new PageResult(
            result.getContent().stream().map(item -> ContributionView.from(
                item,
                jobs.findByContributionIdOrderBySortOrderAscIdAsc(item.getId()),
                false
            )).toList(),
            result.getTotalElements(),
            safePage,
            safeSize
        );
    }

    @Transactional(readOnly = true)
    public ContributionView userDetail(Long userId, Long contributionId) {
        ContentContribution item = contributions.findByIdAndUserId(contributionId, userId)
            .orElseThrow(() -> BusinessException.notFound("共建内容不存在"));
        return ContributionView.from(
            item,
            jobs.findByContributionIdOrderBySortOrderAscIdAsc(item.getId()),
            false
        );
    }

    @Transactional(readOnly = true)
    public PageResult adminList(String status, String keyword, int page, int size) {
        String normalizedStatus = optionalStatus(status);
        String normalizedKeyword = optional(keyword, 40);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<ContentContribution> result = contributions.searchAdmin(
            normalizedStatus,
            normalizedKeyword,
            PageRequest.of(safePage, safeSize)
        );
        return new PageResult(
            result.getContent().stream().map(item -> ContributionView.from(
                item,
                jobs.findByContributionIdOrderBySortOrderAscIdAsc(item.getId()),
                true
            )).toList(),
            result.getTotalElements(),
            safePage,
            safeSize
        );
    }

    @Transactional(readOnly = true)
    public ContributionView adminDetail(Long contributionId) {
        ContentContribution item = contributions.findById(contributionId)
            .orElseThrow(() -> BusinessException.notFound("共建内容不存在"));
        return ContributionView.from(
            item,
            jobs.findByContributionIdOrderBySortOrderAscIdAsc(item.getId()),
            true
        );
    }

    @Transactional(readOnly = true)
    public RawPayloadView original(Long contributionId) {
        ContentContribution item = contributions.findById(contributionId)
            .orElseThrow(() -> BusinessException.notFound("共建内容不存在"));
        return new RawPayloadView(item.getId(), cipher.decrypt(item.getRawPayloadEncrypted()));
    }

    @Transactional
    public ContributionView review(
        AdminUser admin,
        Long contributionId,
        ReviewCommand command,
        String ipAddress
    ) {
        ContentContribution item = contributions.findWithLockById(contributionId)
            .orElseThrow(() -> BusinessException.notFound("共建内容不存在"));
        if (!PENDING.equals(item.getStatus())) {
            throw BusinessException.badRequest("该共建内容已经完成审核，不能再次变更");
        }
        String decision = required(command.decision(), 30, "请选择审核结果").toUpperCase(Locale.ROOT);
        if (!REVIEW_DECISIONS.contains(decision)) {
            throw BusinessException.badRequest("审核结果不正确");
        }

        item.setReviewedByAdmin(admin);
        item.setReviewedAt(LocalDateTime.now());
        item.setStatus(decision);
        if (ADOPTED.equals(decision)) {
            adopt(item, command);
        } else if (REJECTED.equals(decision)) {
            reject(item, command);
        } else {
            rejectAsViolation(item, command);
        }
        item = contributions.saveAndFlush(item);
        audit(admin, item, ipAddress);
        return ContributionView.from(
            item,
            jobs.findByContributionIdOrderBySortOrderAscIdAsc(item.getId()),
            true
        );
    }

    private void adopt(ContentContribution item, ReviewCommand command) {
        String standardName = required(command.standardMatterName(), 40, "请输入对应标准事项");
        String standardKey = normalizeIdentity(standardName);
        if (standardKey.isBlank()) {
            throw BusinessException.badRequest("对应标准事项不正确");
        }
        BigDecimal amount = rewardAmount(command.rewardAmount());
        if (rewards.existsByUserIdAndStandardMatterKey(item.getUser().getId(), standardKey)) {
            throw BusinessException.badRequest("该用户已经因同一标准事项获得过共建奖励");
        }
        if (rewards.existsByCandidateFingerprint(item.getCandidateFingerprint())) {
            throw BusinessException.badRequest("相同共建内容已经被更早的有效提交采用");
        }
        if (contributions.existsByCandidateFingerprintAndStatusAndCreatedAtBefore(
            item.getCandidateFingerprint(),
            PENDING,
            item.getCreatedAt()
        )) {
            throw BusinessException.badRequest("存在提交时间更早的相同候选内容，请先处理更早记录");
        }

        item.setStandardMatterName(sensitiveWords.mask(standardName));
        item.setStandardMatterKey(standardKey);
        item.setRewardAmount(amount);
        item.setReviewReason(optional(command.reason(), 500));
        item.setViolationLevel(null);
        item.setViolationReason(null);

        ContentContributionReward reward = new ContentContributionReward();
        reward.setContribution(item);
        reward.setUser(item.getUser());
        reward.setStandardMatterKey(standardKey);
        reward.setCandidateFingerprint(item.getCandidateFingerprint());
        reward.setRewardAmount(amount);
        rewards.saveAndFlush(reward);
        walletService.creditContentContributionReward(item.getUser().getId(), amount, item.getId());
        notifications.send(
            item.getUser(),
            "共建内容已采用",
            "感谢您的内容共建，" + amount.stripTrailingZeros().toPlainString() +
                "元奖励已计入可提现收入。采用代表内容具有参考价值，不代表原内容会直接上线。",
            "/content-contributions/" + item.getId()
        );
        analytics.recordBusinessAfterCommit(item.getUser(), "content_contribution_adopted", java.util.Map.of(
            "contribution_id", item.getId(),
            "reward_amount", amount.toPlainString()
        ));
        analytics.recordBusinessAfterCommit(item.getUser(), "content_contribution_rewarded", java.util.Map.of(
            "contribution_id", item.getId(),
            "reward_amount", amount.toPlainString()
        ));
    }

    private void reject(ContentContribution item, ReviewCommand command) {
        String reason = required(command.reason(), 500, "请输入驳回原因");
        item.setReviewReason(sensitiveWords.mask(reason));
        item.setStandardMatterName(null);
        item.setStandardMatterKey(null);
        item.setRewardAmount(null);
        item.setViolationLevel(null);
        item.setViolationReason(null);
        notifications.send(
            item.getUser(),
            "共建内容未采用",
            "本次内容未被采用：" + sensitiveWords.mask(reason),
            "/content-contributions/" + item.getId()
        );
        analytics.recordBusinessAfterCommit(item.getUser(), "content_contribution_rejected", java.util.Map.of(
            "contribution_id", item.getId(),
            "violation", false
        ));
    }

    private void rejectAsViolation(ContentContribution item, ReviewCommand command) {
        String reason = required(command.reason(), 500, "请输入违规事实和驳回原因");
        Integer level = command.violationLevel();
        if (level == null || level < 0 || level > 9) {
            throw BusinessException.badRequest("违规等级必须为0至9级");
        }
        String maskedReason = sensitiveWords.mask(reason);
        item.setReviewReason(maskedReason);
        item.setStandardMatterName(null);
        item.setStandardMatterKey(null);
        item.setRewardAmount(null);
        item.setViolationLevel(level);
        item.setViolationReason(maskedReason);
        violations.applyForSource(
            item.getUser().getId(),
            "CONTENT_CONTRIBUTION",
            item.getId(),
            level,
            reason,
            "内容共建不得用于恶意灌水、广告引流、违规内容或套取奖励"
        );
        notifications.send(
            item.getUser(),
            "共建内容违规驳回",
            "本次共建内容未被采用，并已按平台规则记录违规。",
            "/content-contributions/" + item.getId()
        );
        analytics.recordBusinessAfterCommit(item.getUser(), "content_contribution_rejected", java.util.Map.of(
            "contribution_id", item.getId(),
            "violation", true,
            "violation_level", level
        ));
    }

    private ValidatedSubmission validate(SubmitCommand command) {
        if (command == null) throw BusinessException.badRequest("共建内容不能为空");
        String matterName = required(command.matterName(), MAX_MATTER_NAME_LENGTH, "请输入事情名称");
        if (matterName.length() < 2) throw BusinessException.badRequest("事情名称至少需要2个字");
        if (command.jobs() == null || command.jobs().isEmpty()) {
            throw BusinessException.badRequest("至少需要填写1个岗位");
        }
        if (command.jobs().size() > MAX_JOBS) {
            throw BusinessException.badRequest("每条共建内容最多填写10个岗位");
        }
        Set<String> jobNames = new HashSet<>();
        List<ValidatedJob> result = new ArrayList<>();
        for (JobCommand commandJob : command.jobs()) {
            if (commandJob == null) throw BusinessException.badRequest("岗位内容不完整");
            String jobName = required(commandJob.jobName(), 40, "请输入岗位名称");
            if (jobName.length() < 2) throw BusinessException.badRequest("岗位名称至少需要2个字");
            String responsibility = required(
                commandJob.responsibility(),
                300,
                "请输入岗位主要职责"
            );
            if (responsibility.length() < 5) {
                throw BusinessException.badRequest("岗位主要职责至少需要5个字");
            }
            if (!jobNames.add(normalizeIdentity(jobName))) {
                throw BusinessException.badRequest("同一条共建内容中不能重复填写相同岗位");
            }
            result.add(new ValidatedJob(jobName, responsibility));
        }
        return new ValidatedSubmission(matterName, List.copyOf(result));
    }

    private BigDecimal rewardAmount(BigDecimal source) {
        if (source == null) throw BusinessException.badRequest("请选择1至3元共建奖励");
        BigDecimal value = source.setScale(2, RoundingMode.UNNECESSARY);
        if (!Set.of(new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("3.00")).contains(value)) {
            throw BusinessException.badRequest("共建奖励只能选择1元、2元或3元");
        }
        return value;
    }

    private String fingerprint(String matterName, List<ValidatedJob> items) {
        String normalizedJobs = items.stream()
            .map(ValidatedJob::jobName)
            .map(this::normalizeIdentity)
            .sorted(Comparator.naturalOrder())
            .reduce((left, right) -> left + "|" + right)
            .orElse("");
        return sha256(normalizeIdentity(matterName) + "#" + normalizedJobs);
    }

    private String normalizeIdentity(String source) {
        String value = Normalizer.normalize(source == null ? "" : source.trim(), Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT);
        return value.replaceAll("[\\p{P}\\p{S}\\s]+", "");
    }

    private String sha256(String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成共建内容指纹", exception);
        }
    }

    private String toJson(ValidatedSubmission value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法保存共建原始内容", exception);
        }
    }

    private String required(String source, int max, String message) {
        String value = source == null ? "" : source.trim();
        if (value.isEmpty()) throw BusinessException.badRequest(message);
        if (value.length() > max) throw BusinessException.badRequest(message + "，最多" + max + "个字");
        return value;
    }

    private String optional(String source, int max) {
        if (source == null || source.isBlank()) return null;
        String value = source.trim();
        if (value.length() > max) throw BusinessException.badRequest("内容最多" + max + "个字");
        return sensitiveWords.mask(value);
    }

    private String optionalStatus(String source) {
        if (source == null || source.isBlank()) return null;
        String value = source.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(PENDING, ADOPTED, REJECTED, VIOLATION_REJECTED).contains(value)) {
            throw BusinessException.badRequest("共建状态不正确");
        }
        return value;
    }

    private void audit(AdminUser admin, ContentContribution item, String ipAddress) {
        AdminAuditLog audit = new AdminAuditLog();
        audit.setAdminUser(admin);
        audit.setAction("REVIEW_CONTENT_CONTRIBUTION");
        audit.setTargetType("CONTENT_CONTRIBUTION");
        audit.setTargetId(String.valueOf(item.getId()));
        audit.setDetail(
            "审核结果=" + item.getStatus() +
                (item.getRewardAmount() == null ? "" : ",奖励=" + item.getRewardAmount()) +
                (item.getViolationLevel() == null ? "" : ",违规等级=" + item.getViolationLevel())
        );
        audit.setIpAddress(ipAddress);
        auditLogs.save(audit);
    }

    public record JobCommand(String jobName, String responsibility) {
    }

    public record SubmitCommand(String matterName, List<JobCommand> jobs) {
    }

    public record ReviewCommand(
        String decision,
        String standardMatterName,
        BigDecimal rewardAmount,
        String reason,
        Integer violationLevel
    ) {
    }

    private record ValidatedJob(String jobName, String responsibility) {
    }

    private record ValidatedSubmission(String matterName, List<ValidatedJob> jobs) {
    }

    public record UserSummary(
        long totalSubmitted,
        int totalLimit,
        long pendingCount,
        int pendingLimit,
        long adoptedCount,
        BigDecimal earnedReward,
        boolean canSubmit,
        int minimumReward,
        int maximumReward
    ) {
    }

    public record JobView(Long id, String jobName, String responsibility, int sortOrder) {
        static JobView from(ContentContributionJob item) {
            return new JobView(
                item.getId(),
                item.getJobName(),
                item.getResponsibility(),
                item.getSortOrder()
            );
        }
    }

    public record ContributionView(
        Long id,
        String userUid,
        String userName,
        String userPhone,
        String matterName,
        List<JobView> jobs,
        String status,
        String standardMatterName,
        BigDecimal rewardAmount,
        String reviewReason,
        Integer violationLevel,
        String violationReason,
        String reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
    ) {
        static ContributionView from(
            ContentContribution item,
            List<ContentContributionJob> jobs,
            boolean admin
        ) {
            User user = item.getUser();
            String name = user.getNickname() == null || user.getNickname().isBlank()
                ? "UID " + user.getUid()
                : user.getNickname();
            AdminUser reviewer = item.getReviewedByAdmin();
            return new ContributionView(
                item.getId(),
                user.getUid(),
                name,
                admin ? user.getPhone() : null,
                item.getMatterName(),
                jobs.stream().map(JobView::from).toList(),
                item.getStatus(),
                item.getStandardMatterName(),
                item.getRewardAmount(),
                item.getReviewReason(),
                item.getViolationLevel(),
                item.getViolationReason(),
                admin && reviewer != null ? reviewer.getDisplayName() : null,
                item.getReviewedAt(),
                item.getCreatedAt()
            );
        }
    }

    public record PageResult(
        List<ContributionView> items,
        long total,
        int page,
        int size
    ) {
    }

    public record RawPayloadView(Long id, String rawPayloadJson) {
    }
}
