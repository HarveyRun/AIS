package com.shixianwen.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface InquiryMessageRepository extends JpaRepository<InquiryMessage, Long> {
    @EntityGraph(attributePaths = "sender")
    List<InquiryMessage> findByInquiryIdOrderByCreatedAtAsc(Long inquiryId);

    @Override
    @EntityGraph(attributePaths = {"sender", "inquiry", "inquiry.questioner", "inquiry.answerer"})
    Optional<InquiryMessage> findById(Long id);

    @Query("""
        select count(message) from InquiryMessage message
        where message.inquiry.id=:inquiryId and message.sender.id=:senderId
          and message.id > coalesce((
              select max(other.id) from InquiryMessage other
              where other.inquiry.id=:inquiryId
                and other.sender is not null and other.sender.id<>:senderId
          ),0)
        """)
    long countConsecutiveMessages(
        @Param("inquiryId") Long inquiryId,
        @Param("senderId") Long senderId
    );

    long countByInquiryIdAndSenderIdAndCreatedAtAfter(Long inquiryId, Long senderId, LocalDateTime after);

    long countByInquiryIdAndSenderId(Long inquiryId, Long senderId);

    long countByInquiryIdAndSenderIdAndMessageType(Long inquiryId, Long senderId, String messageType);

    long countByInquiryIdAndSenderIdAndMessageTypeIn(
        Long inquiryId,
        Long senderId,
        Collection<String> messageTypes
    );

    long countByInquiryIdAndSenderIdAndCountsTowardFreeLimitTrueAndMessageTypeIn(
        Long inquiryId,
        Long senderId,
        Collection<String> messageTypes
    );

    long countByInquiryIdAndSenderIdAndMessageTypeAndCreatedAtAfter(
        Long inquiryId,
        Long senderId,
        String messageType,
        LocalDateTime after
    );

    long countByInquiryIdAndSenderIdAndContentAndCreatedAtAfter(
        Long inquiryId,
        Long senderId,
        String content,
        LocalDateTime after
    );

    @Query("""
        select message.inquiry.id, max(message.createdAt)
        from InquiryMessage message
        where message.sender.id=:senderId
        group by message.inquiry.id
        """)
    List<Object[]> findLastSentAtBySenderId(@Param("senderId") Long senderId);
}
