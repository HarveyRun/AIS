package com.shixianwen.quality;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InquiryEvaluationRepository extends JpaRepository<InquiryEvaluation, Long> {
    Optional<InquiryEvaluation> findByInquiryId(Long inquiryId);
    boolean existsByInquiryId(Long inquiryId);
}
