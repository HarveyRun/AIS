package com.shixianwen.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findByQuestionerIdOrAnswererIdOrderByCreatedAtDesc(Long questionerId, Long answererId);
    List<Inquiry> findByStatusAndResponseDeadlineBefore(String status, LocalDateTime now);
    List<Inquiry> findByStatusAndConfirmationDeadlineBefore(String status, LocalDateTime now);
    boolean existsByQuestionerIdAndStatusIn(Long userId, Collection<String> statuses);
    boolean existsByAnswererIdAndStatusIn(Long userId, Collection<String> statuses);
    boolean existsByQuestionerIdAndAnswererIdAndStatusIn(Long questionerId, Long answererId, Collection<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inquiry> findWithLockById(Long id);
}
