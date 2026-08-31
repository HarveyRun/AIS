package com.shixianwen.contribution;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ContentContributionRepository extends JpaRepository<ContentContribution, Long> {
    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    Page<ContentContribution> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    Optional<ContentContribution> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from ContentContribution item where item.id=:id")
    Optional<ContentContribution> findWithLockById(@Param("id") Long id);

    boolean existsByCandidateFingerprintAndStatusAndCreatedAtBefore(
        String candidateFingerprint,
        String status,
        LocalDateTime createdAt
    );

    @Query("""
        select item from ContentContribution item join item.user user
        where (:status is null or item.status=:status)
          and (:keyword is null or user.uid like concat('%',:keyword,'%')
            or user.phone like concat('%',:keyword,'%')
            or item.matterName like concat('%',:keyword,'%'))
        order by item.createdAt asc, item.id asc
        """)
    Page<ContentContribution> searchAdmin(
        @Param("status") String status,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
