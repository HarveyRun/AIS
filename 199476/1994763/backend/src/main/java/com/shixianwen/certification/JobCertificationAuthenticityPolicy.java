package com.shixianwen.certification;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class JobCertificationAuthenticityPolicy {
    public static final List<Integer> LEVELS = List.of(100, 90, 80, 60, 51, 40, 20, 0);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日");

    private JobCertificationAuthenticityPolicy() {
    }

    public static Rule resolve(Integer authenticityPercent, LocalDateTime reviewedAt) {
        if (authenticityPercent == null) {
            throw BusinessException.badRequest("请选择材料真实程度");
        }
        int levelIndex = LEVELS.indexOf(authenticityPercent);
        if (levelIndex < 0) {
            throw BusinessException.badRequest("材料真实程度选项不正确");
        }
        int blockedMonths = levelIndex * 6;
        LocalDateTime availableAt = blockedMonths == 0
            ? null
            : reviewedAt.plusMonths(blockedMonths);
        return new Rule(authenticityPercent, blockedMonths, availableAt);
    }

    public static void requireCanApply(User user, LocalDateTime now) {
        LocalDateTime blockedUntil = user.getJobCertificationBlockedUntil();
        if (blockedUntil != null && now.isBefore(blockedUntil)) {
            throw BusinessException.badRequest(
                "您暂时不能申请岗位认证，可于"
                    + blockedUntil.format(DATE_FORMAT)
                    + "后再次申请"
            );
        }
    }

    public record Rule(
        int authenticityPercent,
        int blockedMonths,
        LocalDateTime availableAt
    ) {
    }
}
