package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExperienceTipRepository extends JpaRepository<ExperienceTip, Long> {
    Optional<ExperienceTip> findByPayerIdAndRequestNo(Long payerId, String requestNo);
}
