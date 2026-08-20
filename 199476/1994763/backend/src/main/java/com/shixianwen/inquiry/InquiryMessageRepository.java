package com.shixianwen.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.time.LocalDateTime;

public interface InquiryMessageRepository extends JpaRepository<InquiryMessage, Long> {
    @EntityGraph(attributePaths = "sender")
    List<InquiryMessage> findByInquiryIdOrderByCreatedAtAsc(Long inquiryId);

    long countByInquiryIdAndSenderIdAndCreatedAtAfter(Long inquiryId, Long senderId, LocalDateTime after);

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
}
