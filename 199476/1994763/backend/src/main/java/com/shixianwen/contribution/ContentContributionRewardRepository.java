package com.shixianwen.contribution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ContentContributionRewardRepository extends JpaRepository<ContentContributionReward, Long> {
    boolean existsByUserIdAndStandardMatterKey(Long userId, String standardMatterKey);

    boolean existsByCandidateFingerprint(String candidateFingerprint);

    @Query("select coalesce(sum(item.rewardAmount), 0) from ContentContributionReward item where item.user.id=:userId")
    BigDecimal sumRewardAmountByUserId(@Param("userId") Long userId);
}
