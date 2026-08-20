package com.shixianwen.wallet;

import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletServiceTest {
    @Test
    void repeatedFreezeWithTheSameBusinessReferenceOnlyRunsOnce() {
        WalletAccountRepository wallets = mock(WalletAccountRepository.class);
        WalletTransactionRepository transactions = mock(WalletTransactionRepository.class);
        WalletAccount account = wallet(1L, "100.00", "0.00");
        AtomicReference<WalletTransaction> recorded = new AtomicReference<>();

        when(wallets.findWithLockByUserId(1L)).thenReturn(Optional.of(account));
        when(transactions.findByUserIdAndTransactionTypeAndReferenceTypeAndReferenceId(
            1L, "INQUIRY_FREEZE", "INQUIRY", 88L
        )).thenAnswer(invocation -> Optional.ofNullable(recorded.get()));
        when(transactions.save(any(WalletTransaction.class))).thenAnswer(invocation -> {
            WalletTransaction transaction = invocation.getArgument(0);
            recorded.set(transaction);
            return transaction;
        });

        WalletService service = service(wallets, transactions);
        service.freeze(1L, new BigDecimal("10"), 88L);
        service.freeze(1L, new BigDecimal("10.00"), 88L);

        assertEquals(new BigDecimal("90.00"), account.getAvailableBalance());
        assertEquals(new BigDecimal("10.00"), account.getFrozenBalance());
        verify(transactions, times(1)).save(any(WalletTransaction.class));
    }

    private WalletService service(
        WalletAccountRepository wallets,
        WalletTransactionRepository transactions
    ) {
        return new WalletService(
            wallets,
            transactions,
            mock(BankCardRepository.class),
            mock(WithdrawalRepository.class),
            mock(UserRepository.class),
            new WithdrawalFeePolicy()
        );
    }

    private WalletAccount wallet(Long userId, String available, String frozen) {
        User user = new User();
        user.setId(userId);
        WalletAccount wallet = new WalletAccount();
        wallet.setUser(user);
        wallet.setAvailableBalance(new BigDecimal(available));
        wallet.setFrozenBalance(new BigDecimal(frozen));
        wallet.setTotalWithdrawn(MoneyAmounts.ZERO);
        return wallet;
    }
}
