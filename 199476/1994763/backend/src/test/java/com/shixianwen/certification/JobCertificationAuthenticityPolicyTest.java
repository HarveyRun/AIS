package com.shixianwen.certification;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobCertificationAuthenticityPolicyTest {
    @Test
    void everyLowerAuthenticityLevelAddsSixMonths() {
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 8, 21, 12, 0);
        List<Integer> levels = List.of(100, 90, 80, 60, 51, 40, 20, 0);

        for (int index = 0; index < levels.size(); index++) {
            JobCertificationAuthenticityPolicy.Rule rule =
                JobCertificationAuthenticityPolicy.resolve(levels.get(index), reviewedAt);

            assertThat(rule.blockedMonths()).isEqualTo(index * 6);
            if (index == 0) {
                assertThat(rule.availableAt()).isNull();
            } else {
                assertThat(rule.availableAt()).isEqualTo(reviewedAt.plusMonths(index * 6L));
            }
        }
    }

    @Test
    void applicationsAreRejectedUntilThePlatformRestrictionExpires() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 21, 12, 0);
        User user = new User();
        user.setJobCertificationBlockedUntil(now.plusMonths(6));

        assertThatThrownBy(() -> JobCertificationAuthenticityPolicy.requireCanApply(user, now))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("暂时不能申请岗位认证");

        JobCertificationAuthenticityPolicy.requireCanApply(user, now.plusMonths(6));
    }
}
