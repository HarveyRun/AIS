package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RechargeRepository extends JpaRepository<Recharge, Long> {
    List<Recharge> findByUserIdOrderByCreatedAtDesc(Long userId);
    java.util.Optional<Recharge> findByOrderNo(String orderNo);
}
