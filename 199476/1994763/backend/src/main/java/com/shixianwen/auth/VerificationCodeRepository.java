package com.shixianwen.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findFirstByPhoneOrderByCreatedAtDesc(String phone);

    Optional<VerificationCode> findFirstByPhoneAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
        String phone,
        LocalDateTime now
    );

    long countByPhoneAndCreatedAtAfter(String phone, LocalDateTime after);

    long countByRequestIpAndCreatedAtAfter(String requestIp, LocalDateTime after);
}
