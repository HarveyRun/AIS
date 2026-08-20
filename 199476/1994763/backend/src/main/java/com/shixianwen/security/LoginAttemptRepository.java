package com.shixianwen.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    long countBySubjectTypeAndPhoneAndSuccessfulFalseAndCreatedAtAfter(String type, String phone, LocalDateTime after);
    long countBySubjectTypeAndRequestIpAndSuccessfulFalseAndCreatedAtAfter(String type, String ip, LocalDateTime after);
    long countBySubjectTypeAndDeviceIdAndSuccessfulFalseAndCreatedAtAfter(String type, String deviceId, LocalDateTime after);
}
