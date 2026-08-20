package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlipayAccountRepository extends JpaRepository<AlipayAccount, Long> {
    Optional<AlipayAccount> findByUserId(Long userId);
    Optional<AlipayAccount> findByAlipayUserIdHash(String alipayUserIdHash);
}
