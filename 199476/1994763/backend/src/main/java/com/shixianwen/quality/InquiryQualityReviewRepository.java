package com.shixianwen.quality;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface InquiryQualityReviewRepository extends JpaRepository<InquiryQualityReview, Long> {
    Optional<InquiryQualityReview> findByInquiryId(Long inquiryId);
    boolean existsByInquiryId(Long inquiryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"inquiry", "questioner", "answerer"})
    Optional<InquiryQualityReview> findWithLockById(Long id);
}
