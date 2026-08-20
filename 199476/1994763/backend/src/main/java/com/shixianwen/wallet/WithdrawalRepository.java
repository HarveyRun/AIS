package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findByUserIdOrderByCreatedAtDesc(Long userId);
    java.util.Optional<Withdrawal> findByUserIdAndRequestNo(Long userId, String requestNo);

    @Query("select coalesce(sum(w.amount), 0) from Withdrawal w where w.user.id = :userId and w.status <> 'FAILED' and w.createdAt >= :after")
    BigDecimal sumAmountByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("after") LocalDateTime after);
}
