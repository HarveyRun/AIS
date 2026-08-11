package com.shixianwen.admin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;
public interface AdminSessionRepository extends JpaRepository<AdminSession, Long> {
    Optional<AdminSession> findByTokenHashAndExpiresAtAfter(String hash, LocalDateTime now);
    void deleteByTokenHash(String hash);
}
