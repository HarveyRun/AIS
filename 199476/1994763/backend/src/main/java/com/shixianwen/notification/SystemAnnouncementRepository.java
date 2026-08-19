package com.shixianwen.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SystemAnnouncementRepository
    extends JpaRepository<SystemAnnouncement, Long> {

    Page<SystemAnnouncement> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<SystemAnnouncement> findByIdAndDeletedAtIsNull(Long id);

    List<SystemAnnouncement> findAllByStatusAndDeletedAtIsNull(
        String status
    );

    List<SystemAnnouncement> findAllByStatusAndScheduledAtLessThanEqualAndDeletedAtIsNull(
        String status,
        LocalDateTime scheduledAt
    );
}
