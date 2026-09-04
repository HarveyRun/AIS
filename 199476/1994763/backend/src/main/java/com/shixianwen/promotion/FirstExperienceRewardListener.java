package com.shixianwen.promotion;

import com.shixianwen.certification.ExperienceApproved;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FirstExperienceRewardListener {
    private final FirstExperienceRewardService service;

    @EventListener
    public void onExperienceApproved(ExperienceApproved event) {
        service.reward(event.certificationId());
    }
}
