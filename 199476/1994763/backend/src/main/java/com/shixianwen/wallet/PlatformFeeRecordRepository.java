package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformFeeRecordRepository extends JpaRepository<PlatformFeeRecord, Long> {
    boolean existsByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    java.util.Optional<PlatformFeeRecord> findByReferenceTypeAndReferenceId(
        String referenceType,
        Long referenceId
    );
}
