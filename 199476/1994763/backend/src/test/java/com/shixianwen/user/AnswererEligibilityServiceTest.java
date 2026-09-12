package com.shixianwen.user;

import com.shixianwen.certification.CertificationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnswererEligibilityServiceTest {
    @Test
    void approvedExperienceIsEnoughToAcceptInquiries() {
        UserRepository users = mock(UserRepository.class);
        CertificationRepository certifications = mock(CertificationRepository.class);
        User user = new User();
        user.setId(7L);
        user.setAccountStatus("ACTIVE");
        user.setAcceptingInquiries(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(certifications.existsByUserIdAndCategoryAndStatusAndEnabledTrue(
            7L, "EXPERIENCE", "APPROVED"
        )).thenReturn(true);

        AnswererEligibilityService service = new AnswererEligibilityService(
            users,
            certifications
        );

        AnswererEligibilityService.Eligibility result = service.requireAvailable(7L);

        assertTrue(result.answererQualified());
    }
}
