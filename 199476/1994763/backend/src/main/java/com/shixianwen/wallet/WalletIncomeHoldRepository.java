package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface WalletIncomeHoldRepository extends JpaRepository<WalletIncomeHold, Long> {
    Optional<WalletIncomeHold> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    List<WalletIncomeHold> findTop100ByStatusAndReleaseAtBeforeOrderByReleaseAtAsc(String status, LocalDateTime before);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WalletIncomeHold> findWithLockById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select hold from WalletIncomeHold hold
        where hold.referenceType = :referenceType and hold.referenceId = :referenceId
        """)
    Optional<WalletIncomeHold> findWithLockByReference(
        @Param("referenceType") String referenceType,
        @Param("referenceId") Long referenceId
    );
}
