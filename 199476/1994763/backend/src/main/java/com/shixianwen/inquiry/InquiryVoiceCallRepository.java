package com.shixianwen.inquiry;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InquiryVoiceCallRepository
    extends JpaRepository<InquiryVoiceCall, Long> {

    @EntityGraph(attributePaths = {"inquiry", "questioner", "answerer"})
    Optional<InquiryVoiceCall> findTopByInquiryIdOrderByIdDesc(Long inquiryId);

    @EntityGraph(attributePaths = {"inquiry", "inquiry.questioner", "inquiry.answerer", "questioner", "answerer"})
    @Query("select call from InquiryVoiceCall call where call.id=:id")
    Optional<InquiryVoiceCall> findDetailById(@Param("id") Long id);

    boolean existsByInquiryIdAndStatusIn(Long inquiryId, Collection<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"inquiry", "inquiry.questioner", "inquiry.answerer", "questioner", "answerer"})
    @Query("select call from InquiryVoiceCall call where call.id=:id")
    Optional<InquiryVoiceCall> findWithLockById(@Param("id") Long id);

    List<InquiryVoiceCall> findTop100ByStatusAndMaxEndAtBeforeOrderByMaxEndAtAsc(
        String status,
        LocalDateTime now
    );

    List<InquiryVoiceCall> findTop100ByStatusAndConnectDeadlineBeforeOrderByConnectDeadlineAsc(
        String status,
        LocalDateTime now
    );

    List<InquiryVoiceCall> findTop100ByStatusAndReconnectDeadlineBeforeOrderByReconnectDeadlineAsc(
        String status,
        LocalDateTime now
    );
}
