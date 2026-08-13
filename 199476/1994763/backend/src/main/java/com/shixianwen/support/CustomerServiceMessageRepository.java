package com.shixianwen.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerServiceMessageRepository extends JpaRepository<CustomerServiceMessage, Long> {
    List<CustomerServiceMessage> findByUserIdOrderByCreatedAtAsc(Long userId);
    long countByUserIdAndSenderTypeAndReadFalse(Long userId, String senderType);
}
