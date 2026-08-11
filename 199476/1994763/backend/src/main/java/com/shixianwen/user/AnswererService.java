package com.shixianwen.user;

import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnswererService {
    private final UserRepository userRepository;
    private final CertificationRepository certificationRepository;

    public AnswererService(UserRepository userRepository, CertificationRepository certificationRepository) {
        this.userRepository = userRepository;
        this.certificationRepository = certificationRepository;
    }

    @Transactional(readOnly = true)
    public List<AnswererView> search(String keyword) {
        return userRepository.searchAnswerers(keyword == null ? "" : keyword.trim()).stream()
            .map(this::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public AnswererView detail(String uid) {
        User user = userRepository.findByUidAndAccountStatus(uid, "ACTIVE")
            .filter(item -> "APPROVED".equals(item.getAnswererStatus()))
            .orElseThrow(() -> BusinessException.notFound("该答主不存在"));
        return toView(user);
    }

    private AnswererView toView(User user) {
        List<Certification> certifications =
            certificationRepository.findByUserIdAndStatusOrderByIdAsc(user.getId(), "APPROVED");
        Certification job = certifications.stream()
            .filter(item -> "MAIN_JOB".equals(item.getCertificationType()))
            .findFirst()
            .orElse(null);
        List<ExperienceView> experiences = certifications.stream()
            .filter(item -> "EXPERIENCE".equals(item.getCategory()))
            .map(item -> new ExperienceView(
                item.getId(), item.getTitle(), item.getDescription(), item.getYears(), item.getDiscoveryCategoryId()
            ))
            .toList();
        return new AnswererView(
            user.getId(), user.getUid(), user.getNickname(), user.getAvatarUrl(), user.isAcceptingInquiries(),
            job == null ? null : job.getTitle(), job == null ? null : job.getYears(), user.getCapabilityDescription(), experiences
        );
    }

    public record ExperienceView(
        Long certificationId,
        String title,
        String description,
        Integer years,
        Long discoveryCategoryId
    ) {
    }

    public record AnswererView(
        Long id,
        String uid,
        String nickname,
        String avatarUrl,
        boolean acceptingInquiries,
        String mainJob,
        Integer mainJobYears,
        String capabilityDescription,
        List<ExperienceView> experiences
    ) {
    }
}
