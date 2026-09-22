package com.shixianwen.certification;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExperienceProofUploadRepository extends JpaRepository<ExperienceProofUpload, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExperienceProofUpload> findByIdAndUserId(String id, Long userId);

    long countByUserIdAndStatusAndExpiresAtAfter(Long userId, String status, LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ExperienceProofUpload> findTop100ByStatusInAndExpiresAtBeforeOrderByExpiresAtAsc(
        List<String> statuses, LocalDateTime now
    );
}
