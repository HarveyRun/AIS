package com.shixianwen.wallet;

import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.auth.VerificationCodeService;
import com.shixianwen.auth.AppTestLoginAccountService;
import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.inquiry.Inquiry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletServiceTest {
    @Test
    void experienceUsesUnifiedTipRules() {
        WalletAccountRepository wallets = mock(WalletAccountRepository.class);
        WalletTransactionRepository transactions = mock(WalletTransactionRepository.class);
        ExperienceTipRepository tips = mock(ExperienceTipRepository.class);
        CertificationRepository certifications = mock(CertificationRepository.class);
        WalletAccount payer = sourcedWallet(1L, "10.00", "5.00");
        WalletAccount receiver = sourcedWallet(2L, "0.00", "2.00");
        Certification certification = experience(88L, receiver.getUser());
        AtomicReference<ExperienceTip> savedTip = new AtomicReference<>();

        when(wallets.findWithLockByUserId(1L)).thenReturn(Optional.of(payer));
        when(wallets.findWithLockByUserId(2L)).thenReturn(Optional.of(receiver));
        when(certifications.findById(88L)).thenReturn(Optional.of(certification));
        when(tips.findByPayerIdAndRequestNo(1L, "tip_request_123456"))
            .thenAnswer(invocation -> Optional.ofNullable(savedTip.get()));
        when(tips.save(any(ExperienceTip.class))).thenAnswer(invocation -> {
            ExperienceTip tip = invocation.getArgument(0);
            tip.setId(77L);
            savedTip.set(tip);
            return tip;
        });

        WalletService service = service(wallets, transactions, tips, certifications);
        service.tipExperience(1L, 88L, new BigDecimal("3"), "tip_request_123456");
        service.tipExperience(1L, 88L, new BigDecimal("3.00"), "tip_request_123456");

        assertEquals(new BigDecimal("12.00"), payer.getAvailableBalance());
        assertEquals(new BigDecimal("7.00"), payer.getRechargeBalance());
        assertEquals(new BigDecimal("5.00"), payer.getIncomeBalance());
        assertEquals(new BigDecimal("2.00"), receiver.getIncomeBalance());
        assertEquals(new BigDecimal("2.00"), receiver.getAvailableBalance());
        assertEquals(new BigDecimal("2.85"), receiver.getPendingIncomeBalance());
        assertEquals(new BigDecimal("0.15"), savedTip.get().getFeeAmount());
        assertEquals(new BigDecimal("2.85"), savedTip.get().getReceiverIncomeAmount());
        verify(tips, times(1)).save(any(ExperienceTip.class));
        verify(transactions, times(2)).save(any(WalletTransaction.class));
    }

    @Test
    void experienceTipRejectsAmountAboveUnifiedLimit() {
        WalletAccountRepository wallets = mock(WalletAccountRepository.class);
        CertificationRepository certifications = mock(CertificationRepository.class);
        User receiver = new User();
        receiver.setId(2L);
        Certification certification = experience(88L, receiver);
        when(certifications.findById(88L)).thenReturn(Optional.of(certification));

        WalletService service = service(
            wallets,
            mock(WalletTransactionRepository.class),
            mock(ExperienceTipRepository.class),
            certifications
        );

        assertThrows(
            RuntimeException.class,
            () -> service.tipExperience(1L, 88L, new BigDecimal("5001"), "tip_request_123456")
        );
        verify(wallets, times(0)).findWithLockByUserId(any());
    }

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
        when(feeRecords.existsByReferenceTypeAndReferenceId("INQUIRY", 8L)).thenReturn(false);

        Inquiry inquiry = new Inquiry();
        inquiry.setId(8L);
        inquiry.setServiceFeeRate(new BigDecimal("0.050000"));
        inquiry.setServiceFeeAmount(new BigDecimal("5.00"));
        inquiry.setAnswererIncomeAmount(new BigDecimal("95.00"));

        WalletService service = new WalletService(
            wallets,
            transactions,
            mock(ExperienceTipRepository.class),
            mock(com.shixianwen.certification.CertificationRepository.class),
            mock(AlipayAccountRepository.class),
            mock(WithdrawalRepository.class),
            incomeHolds,
            mock(UserRepository.class),
            mock(PlatformServiceFeePolicy.class),
            feeRecords,
            mock(VerificationCodeService.class),
            mock(AppTestLoginAccountService.class),
            mock(SecurityEventService.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class),
            mock(com.shixianwen.finance.FinancialLedgerService.class),
            mock(org.springframework.jdbc.core.JdbcTemplate.class)
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

    private WalletService service(
        WalletAccountRepository wallets,
        WalletTransactionRepository transactions
    ) {
        return service(
            wallets,
            transactions,
            mock(ExperienceTipRepository.class),
            mock(CertificationRepository.class)
        );
    }

    private WalletService service(
        WalletAccountRepository wallets,
        WalletTransactionRepository transactions,
        ExperienceTipRepository tips,
        CertificationRepository certifications
    ) {
        PlatformServiceFeePolicy feePolicy = mock(PlatformServiceFeePolicy.class);
        when(feePolicy.quote(any(BigDecimal.class), any())).thenAnswer(invocation -> {
            BigDecimal amount = invocation.getArgument(0);
            BigDecimal fee = MoneyAmounts.normalize(amount.multiply(new BigDecimal("0.050000")));
            return new PlatformServiceFeePolicy.SettlementQuote(
                "ANDROID", amount, new BigDecimal("0.050000"), fee,
                MoneyAmounts.subtract(amount, fee)
            );
        });
        when(feePolicy.currentRate(any())).thenReturn(new BigDecimal("0.050000"));
        return new WalletService(
            wallets,
            transactions,
            tips,
            certifications,
            mock(AlipayAccountRepository.class),
            mock(WithdrawalRepository.class),
            mock(WalletIncomeHoldRepository.class),
            mock(UserRepository.class),
            feePolicy,
            mock(PlatformFeeRecordRepository.class),
            mock(VerificationCodeService.class),
            mock(AppTestLoginAccountService.class),
            mock(SecurityEventService.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class),
            mock(com.shixianwen.finance.FinancialLedgerService.class),
            mock(org.springframework.jdbc.core.JdbcTemplate.class)
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

    private WalletAccount sourcedWallet(Long userId, String recharge, String income) {
        WalletAccount wallet = wallet(userId, "0.00", "0.00");
        wallet.getUser().setAccountType("NORMAL");
        wallet.setRechargeBalance(new BigDecimal(recharge));
        wallet.setIncomeBalance(new BigDecimal(income));
        wallet.setPendingIncomeBalance(MoneyAmounts.ZERO);
        wallet.setFrozenRechargeBalance(MoneyAmounts.ZERO);
        wallet.setFrozenIncomeBalance(MoneyAmounts.ZERO);
        wallet.setAvailableBalance(MoneyAmounts.add(wallet.getRechargeBalance(), wallet.getIncomeBalance()));
        return wallet;
    }

    private Certification experience(Long id, User owner) {
        Certification certification = new Certification();
        certification.setId(id);
        certification.setUser(owner);
        certification.setCategory("EXPERIENCE");
        certification.setStatus("APPROVED");
        certification.setEnabled(true);
        return certification;
    }
}
