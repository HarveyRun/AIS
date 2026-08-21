package com.shixianwen.certification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findByUserIdOrderByIdAsc(Long userId);
    List<Certification> findByUserIdAndStatusOrderByIdAsc(Long userId, String status);
    List<Certification> findByUserIdAndStatusAndEnabledTrueOrderByIdAsc(Long userId, String status);
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
}
