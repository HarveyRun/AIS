package com.shixianwen.user;

import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnswererEligibilityService {
    private final UserRepository users;
    private final CertificationRepository certifications;

    public AnswererEligibilityService(
        UserRepository users,
        CertificationRepository certifications
    ) {
        this.users = users;
        this.certifications = certifications;
    }

    @Transactional(readOnly = true)
    public Eligibility current(Long userId) {
        User user = users.findById(userId)
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        boolean identityApproved = approved(userId, "IDENTITY");
        boolean experienceApproved = certifications
            .existsByUserIdAndCategoryAndExperienceBusinessTypeAndStatusAndEnabledTrue(
                userId, "EXPERIENCE", "MONETIZED", "APPROVED"
            );
        return new Eligibility(
            identityApproved,
            experienceApproved,
            identityApproved,
            identityApproved && experienceApproved,
            "ACTIVE".equals(user.getAccountStatus()),
            user.isAcceptingInquiries()
        );
    }

    public Eligibility requireQualified(Long userId) {
        Eligibility eligibility = current(userId);
        if (!eligibility.accountActive()) {
            throw BusinessException.forbidden("当前账号不可用");
        }
        if (!eligibility.answererQualified()) {
            throw BusinessException.forbidden("完成实名认证并通过至少一条亲身经历后才能接受询问");
        }
        return eligibility;
    }

    public Eligibility requireAvailable(Long userId) {
        Eligibility eligibility = requireQualified(userId);
        if (!eligibility.acceptingInquiries()) {
            throw BusinessException.badRequest("对方暂不接受询问");
        }
        return eligibility;
    }

    public Eligibility requireCanAccept(Long userId) {
        Eligibility eligibility = requireQualified(userId);
        if (!eligibility.acceptingInquiries()) {
            throw BusinessException.badRequest("你已暂停接受询问");
        }
        return eligibility;
    }

    private boolean approved(Long userId, String type) {
        return certifications.existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            userId,
            type,
            "APPROVED"
        );
    }

    public record Eligibility(
        boolean identityApproved,
        boolean experienceApproved,
        boolean basicInformationApproved,
        boolean answererQualified,
        boolean accountActive,
        boolean acceptingInquiries
    ) {
    }
}
