package com.shixianwen.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findFirstByPhoneAndPurposeOrderByCreatedAtDesc(String phone, String purpose);

    Optional<VerificationCode> findFirstByPhoneAndPurposeAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
        String phone,
        String purpose,
        LocalDateTime now
    );

    long countByPhoneAndPurposeAndCreatedAtAfter(String phone, String purpose, LocalDateTime after);

    long countByRequestIpAndCreatedAtAfter(String requestIp, LocalDateTime after);

    long countByRequestDeviceIdAndCreatedAtAfter(String requestDeviceId, LocalDateTime after);
}
