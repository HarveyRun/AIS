package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.List;

public interface RechargeRepository extends JpaRepository<Recharge, Long> {
    List<Recharge> findByUserIdOrderByCreatedAtDesc(Long userId);
    java.util.Optional<Recharge> findByOrderNo(String orderNo);
    java.util.Optional<Recharge> findByProviderTradeNo(String providerTradeNo);
    java.util.Optional<Recharge> findByUserIdAndRequestNo(Long userId, String requestNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<Recharge> findWithLockByOrderNo(String orderNo);
}
