package com.shixianwen.banner;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface HomeBannerRepository extends JpaRepository<HomeBanner, Long> {
    Page<HomeBanner> findAllByDeletedFalse(Pageable pageable);

    Optional<HomeBanner> findByIdAndDeletedFalse(Long id);

    boolean existsByActionTypeAndDeletedFalseAndEnabledTrueAndStartAtLessThanEqualAndEndAtGreaterThan(
        String actionType,
        LocalDateTime startAt,
        LocalDateTime endAt
    );

    List<HomeBanner> findAllByDeletedFalseAndEnabledTrueAndStartAtLessThanEqualAndEndAtGreaterThanOrderBySortOrderAscIdAsc(
        LocalDateTime startAt,
        LocalDateTime endAt
    );
}
