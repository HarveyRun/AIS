package com.shixianwen.promotion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FirstExperienceRewardRepository
    extends JpaRepository<FirstExperienceReward, Long> {
    boolean existsByUserId(Long userId);
}
