package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WalletTransaction> findByUserIdAndTransactionTypeAndReferenceTypeAndReferenceId(
        Long userId,
        String transactionType,
        String referenceType,
        Long referenceId
    );
}
