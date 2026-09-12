package com.shixianwen.config;

import com.shixianwen.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppGlobalSettingService {
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public Settings current() {
        return jdbc.queryForObject(
            "SELECT * FROM app_global_settings WHERE id=1",
            (rs, rowNum) -> new Settings(
                rs.getBoolean("first_experience_reward_enabled"),
                rs.getBigDecimal("first_experience_reward_amount"),
                rs.getTimestamp("first_experience_reward_start_at") == null ? null : rs.getTimestamp("first_experience_reward_start_at").toLocalDateTime(),
                rs.getTimestamp("first_experience_reward_end_at") == null ? null : rs.getTimestamp("first_experience_reward_end_at").toLocalDateTime(),
                rs.getBigDecimal("inquiry_deposit_amount"),
                rs.getInt("free_pending_inquiry_limit"),
                rs.getInt("income_hold_hours"),
                rs.getBigDecimal("withdrawal_min_amount"),
                rs.getBigDecimal("withdrawal_max_amount"),
                rs.getBigDecimal("withdrawal_daily_limit"),
                rs.getInt("payout_account_cooldown_hours"),
                parseAmounts(rs.getString("tip_preset_amounts")),
                rs.getInt("tip_max_amount"),
                rs.getInt("initial_text_message_limit"),
                rs.getInt("text_message_max_length"),
                rs.getInt("voice_reward_seconds"),
                rs.getInt("voice_reward_messages"),
                rs.getInt("voice_ring_timeout_seconds"),
                rs.getInt("voice_reconnect_timeout_seconds"),
                rs.getInt("inquiry_response_timeout_hours"),
                rs.getInt("inquiry_max_duration_days"),
                rs.getInt("hourly_rate_min"),
                rs.getInt("hourly_rate_max"),
                rs.getInt("max_unapproved_experiences"),
                rs.getInt("experience_title_max_length"),
                rs.getInt("experience_description_max_length"),
                rs.getLong("proof_archive_max_bytes"),
                rs.getInt("home_page_size"),
                rs.getInt("curated_membership_months"),
                rs.getBigDecimal("curated_membership_price"),
                rs.getBoolean("experience_publish_enabled"),
                rs.getBoolean("inquiry_enabled"),
                rs.getBoolean("voice_call_enabled"),
                rs.getBoolean("experience_tip_enabled"),
                rs.getBoolean("recharge_enabled"),
                rs.getBoolean("withdrawal_enabled"),
                rs.getObject("updated_by_admin_id", Long.class),
                rs.getTimestamp("updated_at").toLocalDateTime()
            )
        );
    }

    @Transactional
    public Settings update(Settings value, Long adminId) {
        validate(value);
        int changed = jdbc.update(
            "UPDATE app_global_settings SET first_experience_reward_enabled=?,first_experience_reward_amount=?," +
                "first_experience_reward_start_at=?,first_experience_reward_end_at=?," +
                "inquiry_deposit_amount=?,free_pending_inquiry_limit=?,income_hold_hours=?," +
                "withdrawal_min_amount=?,withdrawal_max_amount=?,withdrawal_daily_limit=?," +
                "payout_account_cooldown_hours=?,tip_preset_amounts=?,tip_max_amount=?," +
                "initial_text_message_limit=?,text_message_max_length=?,voice_reward_seconds=?," +
                "voice_reward_messages=?,voice_ring_timeout_seconds=?,voice_reconnect_timeout_seconds=?," +
                "inquiry_response_timeout_hours=?,inquiry_max_duration_days=?,hourly_rate_min=?," +
                "hourly_rate_max=?,max_unapproved_experiences=?,experience_title_max_length=?," +
                "experience_description_max_length=?,proof_archive_max_bytes=?,home_page_size=?,curated_membership_months=?,curated_membership_price=?," +
                "experience_publish_enabled=?,inquiry_enabled=?,voice_call_enabled=?," +
                "experience_tip_enabled=?,recharge_enabled=?,withdrawal_enabled=?,updated_by_admin_id=? WHERE id=1",
            value.firstExperienceRewardEnabled(), value.firstExperienceRewardAmount(),
            value.firstExperienceRewardStartAt(), value.firstExperienceRewardEndAt(),
            value.inquiryDepositAmount(), value.freePendingInquiryLimit(), value.incomeHoldHours(),
            value.withdrawalMinAmount(), value.withdrawalMaxAmount(), value.withdrawalDailyLimit(),
            value.payoutAccountCooldownHours(), joinAmounts(value.tipPresetAmounts()), value.tipMaxAmount(),
            value.initialTextMessageLimit(), value.textMessageMaxLength(), value.voiceRewardSeconds(),
            value.voiceRewardMessages(), value.voiceRingTimeoutSeconds(), value.voiceReconnectTimeoutSeconds(),
            value.inquiryResponseTimeoutHours(), value.inquiryMaxDurationDays(), value.hourlyRateMin(),
            value.hourlyRateMax(), value.maxUnapprovedExperiences(), value.experienceTitleMaxLength(),
            value.experienceDescriptionMaxLength(), value.proofArchiveMaxBytes(), value.homePageSize(), value.curatedMembershipMonths(), value.curatedMembershipPrice(),
            value.experiencePublishEnabled(), value.inquiryEnabled(), value.voiceCallEnabled(),
            value.experienceTipEnabled(), value.rechargeEnabled(), value.withdrawalEnabled(), adminId
        );
        if (changed != 1) throw BusinessException.badRequest("App全局设置不存在");
        return current();
    }

    public boolean firstExperienceRewardActive(Settings settings, LocalDateTime now) {
        return settings.firstExperienceRewardEnabled()
            && (settings.firstExperienceRewardStartAt() == null || !now.isBefore(settings.firstExperienceRewardStartAt()))
            && (settings.firstExperienceRewardEndAt() == null || !now.isAfter(settings.firstExperienceRewardEndAt()));
    }

    private void validate(Settings s) {
        money(s.firstExperienceRewardAmount(), BigDecimal.ZERO, new BigDecimal("9999"), "首次经历奖励");
        if (s.firstExperienceRewardStartAt() != null && s.firstExperienceRewardEndAt() != null
            && !s.firstExperienceRewardEndAt().isAfter(s.firstExperienceRewardStartAt())) {
            throw BusinessException.badRequest("首次经历奖励结束时间必须晚于开始时间");
        }
        money(s.inquiryDepositAmount(), BigDecimal.ZERO, new BigDecimal("9999"), "询问押金");
        money(s.withdrawalMinAmount(), BigDecimal.ONE, new BigDecimal("9999"), "单笔提现最低金额");
        money(s.withdrawalMaxAmount(), s.withdrawalMinAmount(), new BigDecimal("999999"), "单笔提现最高金额");
        money(s.withdrawalDailyLimit(), s.withdrawalMaxAmount(), new BigDecimal("9999999"), "每日提现上限");
        integer(s.freePendingInquiryLimit(), 0, 20, "免费待处理询问数量");
        integer(s.incomeHoldHours(), 0, 720, "收入冻结小时数");
        integer(s.payoutAccountCooldownHours(), 0, 720, "支付宝授权冷静期");
        integer(s.tipMaxAmount(), 1, 9999, "打赏金额上限");
        if (s.tipPresetAmounts() == null || s.tipPresetAmounts().isEmpty() || s.tipPresetAmounts().size() > 10
            || s.tipPresetAmounts().stream().anyMatch(value -> value < 1 || value > s.tipMaxAmount())) {
            throw BusinessException.badRequest("打赏预设金额须填写1至10个不超过打赏上限的正整数");
        }
        integer(s.initialTextMessageLimit(), 1, 500, "初始文字条数");
        integer(s.textMessageMaxLength(), 10, 1000, "单条文字字数");
        integer(s.voiceRewardSeconds(), 60, 86400, "语音奖励所需秒数");
        integer(s.voiceRewardMessages(), 1, 500, "语音奖励文字条数");
        integer(s.voiceRingTimeoutSeconds(), 10, 300, "语音呼叫等待秒数");
        integer(s.voiceReconnectTimeoutSeconds(), 10, 300, "语音重连等待秒数");
        integer(s.inquiryResponseTimeoutHours(), 1, 720, "询问待处理小时数");
        integer(s.inquiryMaxDurationDays(), 1, 365, "询问最长天数");
        integer(s.hourlyRateMin(), 1, 5000, "每小时费用下限");
        integer(s.hourlyRateMax(), s.hourlyRateMin(), 99999, "每小时费用上限");
        integer(s.maxUnapprovedExperiences(), 1, 50, "未通过经历数量上限");
        integer(s.experienceTitleMaxLength(), 5, 100, "经历标题字数");
        integer(s.experienceDescriptionMaxLength(), 20, 5000, "经历叙述字数");
        if (s.proofArchiveMaxBytes() < 1024L * 1024 || s.proofArchiveMaxBytes() > 2L * 1024 * 1024 * 1024) {
            throw BusinessException.badRequest("证明资料上限须在1MB至2GB之间");
        }
        integer(s.homePageSize(), 5, 50, "首页每页数量");
        integer(s.curatedMembershipMonths(), 1, 1200, "严选直聊购买期限");
        money(s.curatedMembershipPrice(), BigDecimal.ONE, new BigDecimal("9999"), "严选直聊开通金额");
    }

    private void money(BigDecimal value, BigDecimal min, BigDecimal max, String label) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw BusinessException.badRequest(label + "配置不正确");
        }
    }

    private void integer(int value, int min, int max, String label) {
        if (value < min || value > max) throw BusinessException.badRequest(label + "配置不正确");
    }

    private static List<Integer> parseAmounts(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
            .map(String::trim).filter(text -> !text.isEmpty()).map(Integer::parseInt).distinct().sorted().toList();
    }

    private static String joinAmounts(List<Integer> values) {
        return values.stream().distinct().sorted().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
    }

    public record Settings(
        boolean firstExperienceRewardEnabled,
        BigDecimal firstExperienceRewardAmount,
        LocalDateTime firstExperienceRewardStartAt,
        LocalDateTime firstExperienceRewardEndAt,
        BigDecimal inquiryDepositAmount,
        int freePendingInquiryLimit,
        int incomeHoldHours,
        BigDecimal withdrawalMinAmount,
        BigDecimal withdrawalMaxAmount,
        BigDecimal withdrawalDailyLimit,
        int payoutAccountCooldownHours,
        List<Integer> tipPresetAmounts,
        int tipMaxAmount,
        int initialTextMessageLimit,
        int textMessageMaxLength,
        int voiceRewardSeconds,
        int voiceRewardMessages,
        int voiceRingTimeoutSeconds,
        int voiceReconnectTimeoutSeconds,
        int inquiryResponseTimeoutHours,
        int inquiryMaxDurationDays,
        int hourlyRateMin,
        int hourlyRateMax,
        int maxUnapprovedExperiences,
        int experienceTitleMaxLength,
        int experienceDescriptionMaxLength,
        long proofArchiveMaxBytes,
        int homePageSize,
        int curatedMembershipMonths,
        BigDecimal curatedMembershipPrice,
        boolean experiencePublishEnabled,
        boolean inquiryEnabled,
        boolean voiceCallEnabled,
        boolean experienceTipEnabled,
        boolean rechargeEnabled,
        boolean withdrawalEnabled,
        Long updatedByAdminId,
        LocalDateTime updatedAt
    ) {}
}
