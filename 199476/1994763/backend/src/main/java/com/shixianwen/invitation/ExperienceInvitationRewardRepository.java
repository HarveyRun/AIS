package com.shixianwen.invitation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExperienceInvitationRewardRepository
    extends JpaRepository<ExperienceInvitationReward, Long> {

    Optional<ExperienceInvitationReward> findByInvitedUserId(Long invitedUserId);
}
