package com.shixianwen.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    @EntityGraph(attributePaths = {"questioner", "answerer"})
    List<Inquiry> findByQuestionerIdOrAnswererIdOrderByCreatedAtDesc(Long questionerId, Long answererId);

    @Override
    @EntityGraph(attributePaths = {"questioner", "answerer"})
    Optional<Inquiry> findById(Long id);
    List<Inquiry> findByStatusAndResponseDeadlineBefore(String status, LocalDateTime now);
    List<Inquiry> findByStatusAndReplyDeadlineBefore(String status, LocalDateTime now);
    boolean existsByQuestionerIdAndStatusIn(Long userId, Collection<String> statuses);
    boolean existsByAnswererIdAndStatusIn(Long userId, Collection<String> statuses);
    long countByQuestionerIdAndStatusIn(Long userId, Collection<String> statuses);
    long countByAnswererIdAndStatusIn(Long userId, Collection<String> statuses);
    boolean existsByQuestionerIdAndIdNotAndStatusIn(Long userId, Long inquiryId, Collection<String> statuses);
    boolean existsByAnswererIdAndIdNotAndStatusIn(Long userId, Long inquiryId, Collection<String> statuses);
    long countByQuestionerIdAndStatus(Long userId, String status);
    boolean existsByQuestionerIdAndAnswererIdAndStatusIn(Long questionerId, Long answererId, Collection<String> statuses);
    long countByQuestionerIdAndCreatedAtAfter(Long questionerId, LocalDateTime after);
    long countByQuestionerIdAndAnswererIdAndCreatedAtAfter(
        Long questionerId,
        Long answererId,
        LocalDateTime after
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"questioner", "answerer"})
    Optional<Inquiry> findWithLockById(Long id);
}
