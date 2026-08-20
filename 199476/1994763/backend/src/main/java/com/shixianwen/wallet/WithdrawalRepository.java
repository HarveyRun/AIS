package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findByUserIdOrderByCreatedAtDesc(Long userId);
    java.util.Optional<Withdrawal> findByUserIdAndRequestNo(Long userId, String requestNo);
}
