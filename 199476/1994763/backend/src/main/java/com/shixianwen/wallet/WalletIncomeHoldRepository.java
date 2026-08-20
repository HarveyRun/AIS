package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface WalletIncomeHoldRepository extends JpaRepository<WalletIncomeHold, Long> {
    Optional<WalletIncomeHold> findByInquiryId(Long inquiryId);
    List<WalletIncomeHold> findTop100ByStatusAndReleaseAtBeforeOrderByReleaseAtAsc(String status, LocalDateTime before);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WalletIncomeHold> findWithLockById(Long id);
}
