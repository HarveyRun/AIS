package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankCardRepository extends JpaRepository<BankCard, Long> {
    Optional<BankCard> findByUserId(Long userId);
}
