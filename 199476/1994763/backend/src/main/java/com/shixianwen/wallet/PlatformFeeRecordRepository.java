package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformFeeRecordRepository extends JpaRepository<PlatformFeeRecord, Long> {
    boolean existsByInquiryId(Long inquiryId);
}
