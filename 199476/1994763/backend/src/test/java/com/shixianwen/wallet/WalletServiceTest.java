package com.shixianwen.wallet;

import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.auth.VerificationCodeService;
import com.shixianwen.auth.AppTestLoginAccountService;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.inquiry.Inquiry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

    @Test
    void settlementCreditsOnlyTheAnswererNetIncome() {
        WalletAccountRepository wallets = mock(WalletAccountRepository.class);
        WalletTransactionRepository transactions = mock(WalletTransactionRepository.class);
        WalletIncomeHoldRepository incomeHolds = mock(WalletIncomeHoldRepository.class);
        PlatformFeeRecordRepository feeRecords = mock(PlatformFeeRecordRepository.class);
        WalletAccount payer = wallet(1L, "0.00", "100.00");
        WalletAccount receiver = wallet(2L, "0.00", "0.00");
        when(wallets.findWithLockByUserId(1L)).thenReturn(Optional.of(payer));
        when(wallets.findWithLockByUserId(2L)).thenReturn(Optional.of(receiver));
        when(transactions.findByUserIdAndTransactionTypeAndReferenceTypeAndReferenceId(
            any(), any(), any(), any()
        )).thenReturn(Optional.empty());
        when(feeRecords.existsByInquiryId(8L)).thenReturn(false);

        Inquiry inquiry = new Inquiry();
        inquiry.setId(8L);
        inquiry.setServiceFeeRate(new BigDecimal("0.050000"));
        inquiry.setServiceFeeAmount(new BigDecimal("5.00"));
        inquiry.setAnswererIncomeAmount(new BigDecimal("95.00"));

        WalletService service = new WalletService(
            wallets,
            transactions,
            mock(AlipayAccountRepository.class),
            mock(WithdrawalRepository.class),
            incomeHolds,
            mock(UserRepository.class),
            mock(PlatformServiceFeePolicy.class),
            feeRecords,
            mock(VerificationCodeService.class),
            mock(AppTestLoginAccountService.class),
            mock(SecurityEventService.class)
        );

        service.settle(
            1L,
            2L,
            new BigDecimal("100.00"),
            MoneyAmounts.ZERO,
            inquiry
        );

        assertEquals(new BigDecimal("0.00"), payer.getFrozenBalance());
        assertEquals(new BigDecimal("95.00"), receiver.getPendingIncomeBalance());

        ArgumentCaptor<WalletIncomeHold> holdCaptor = ArgumentCaptor.forClass(WalletIncomeHold.class);
        verify(incomeHolds).save(holdCaptor.capture());
        assertEquals(new BigDecimal("95.00"), holdCaptor.getValue().getAmount());

        ArgumentCaptor<PlatformFeeRecord> feeCaptor = ArgumentCaptor.forClass(PlatformFeeRecord.class);
        verify(feeRecords).save(feeCaptor.capture());
        assertEquals(new BigDecimal("5.00"), feeCaptor.getValue().getServiceFeeAmount());
        assertEquals(new BigDecimal("95.00"), feeCaptor.getValue().getAnswererIncomeAmount());
    }

    @Test
    void invitationRewardCreditsRechargeBalanceAndIsIdempotent() {
        WalletAccountRepository wallets = mock(WalletAccountRepository.class);
        WalletTransactionRepository transactions = mock(WalletTransactionRepository.class);
        WalletAccount account = wallet(2L, "0.00", "0.00");
        AtomicReference<WalletTransaction> recorded = new AtomicReference<>();

        when(wallets.findWithLockByUserId(2L)).thenReturn(Optional.of(account));
        when(transactions.findByUserIdAndTransactionTypeAndReferenceTypeAndReferenceId(
            2L, "INVITATION_REWARD", "USER_INVITATION", 18L
        )).thenAnswer(invocation -> Optional.ofNullable(recorded.get()));
        when(transactions.save(any(WalletTransaction.class))).thenAnswer(invocation -> {
            WalletTransaction transaction = invocation.getArgument(0);
            recorded.set(transaction);
            return transaction;
        });

        WalletService service = service(wallets, transactions);
        service.creditInvitationReward(2L, new BigDecimal("3"), 18L);
        service.creditInvitationReward(2L, new BigDecimal("3.00"), 18L);

        assertEquals(new BigDecimal("3.00"), account.getIncomeBalance());
        assertEquals(new BigDecimal("3.00"), account.getAvailableBalance());
        verify(transactions, times(1)).save(any(WalletTransaction.class));
    }

    private WalletService service(
        WalletAccountRepository wallets,
        WalletTransactionRepository transactions
    ) {
        return new WalletService(
            wallets,
            transactions,
            mock(AlipayAccountRepository.class),
            mock(WithdrawalRepository.class),
            mock(WalletIncomeHoldRepository.class),
            mock(UserRepository.class),
            mock(PlatformServiceFeePolicy.class),
            mock(PlatformFeeRecordRepository.class),
            mock(VerificationCodeService.class),
            mock(AppTestLoginAccountService.class),
            mock(SecurityEventService.class)
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
