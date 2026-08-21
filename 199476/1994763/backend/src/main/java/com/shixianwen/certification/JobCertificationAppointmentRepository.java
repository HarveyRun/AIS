package com.shixianwen.certification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JobCertificationAppointmentRepository
        extends JpaRepository<JobCertificationAppointment, Long> {
    boolean existsByAppointmentAtAndStatus(LocalDateTime appointmentAt, String status);

    Optional<JobCertificationAppointment>
        findFirstByUserIdAndStatusOrderByAppointmentAtDesc(
            Long userId,
            String status
        );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<JobCertificationAppointment> findWithLockById(Long id);
}
