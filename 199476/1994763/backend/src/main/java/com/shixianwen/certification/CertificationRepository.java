package com.shixianwen.certification;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findByUserIdOrderByIdAsc(Long userId);
    List<Certification> findByUserIdAndStatusOrderByIdAsc(Long userId, String status);
    List<Certification> findByUserIdAndStatusAndEnabledTrueOrderByIdAsc(Long userId, String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Certification> findByIdAndUserId(Long id, Long userId);
    Optional<Certification> findFirstByUserIdAndCertificationTypeOrderByIdDesc(Long userId, String certificationType);
    Optional<Certification> findFirstByUserIdAndCertificationTypeAndStatusAndEnabledTrueOrderByIdDesc(
        Long userId,
        String certificationType,
        String status
    );
    boolean existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
        Long userId,
        String certificationType,
        String status
    );
    long countByUserIdAndCategoryAndStatusNot(
        Long userId,
        String category,
        String status
    );

    boolean existsByUpgradeSourceIdAndDeletedAtIsNull(Long upgradeSourceId);

    boolean existsByUserIdAndCategoryAndExperienceBusinessTypeAndStatusAndEnabledTrue(
        Long userId,
        String category,
        String experienceBusinessType,
        String status
    );

    boolean existsByUserIdAndCategoryAndStatusAndIdNot(
        Long userId,
        String category,
        String status,
        Long id
    );

    Optional<Certification>
        findFirstByUserIdAndCategoryAndExperienceBusinessTypeAndStatusAndEnabledTrueOrderByIdAsc(
            Long userId,
            String category,
            String experienceBusinessType,
            String status
        );
}
