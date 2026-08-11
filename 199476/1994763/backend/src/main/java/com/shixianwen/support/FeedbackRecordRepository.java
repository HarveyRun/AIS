package com.shixianwen.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRecordRepository extends JpaRepository<FeedbackRecord, Long> {
    List<FeedbackRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
