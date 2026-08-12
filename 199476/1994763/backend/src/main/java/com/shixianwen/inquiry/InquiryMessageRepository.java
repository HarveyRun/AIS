package com.shixianwen.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface InquiryMessageRepository extends JpaRepository<InquiryMessage, Long> {
    @EntityGraph(attributePaths = "sender")
    List<InquiryMessage> findByInquiryIdOrderByCreatedAtAsc(Long inquiryId);
}
