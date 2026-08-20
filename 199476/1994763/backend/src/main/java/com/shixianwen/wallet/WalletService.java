package com.shixianwen.wallet;

import com.shixianwen.auth.VerificationCodeService;
import com.shixianwen.auth.AppTestLoginAccountService;
import com.shixianwen.common.BusinessException;
import com.shixianwen.inquiry.Inquiry;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    private static final BigDecimal DAILY_WITHDRAWAL_LIMIT = new BigDecimal("20000.00");
    private static final long PAYOUT_ACCOUNT_COOLDOWN_HOURS = 24;

    private final WalletAccountRepository wallets;
    private final WalletTransactionRepository transactions;
    private final AlipayAccountRepository alipayAccounts;
    private final WithdrawalRepository withdrawals;
    private final WalletIncomeHoldRepository incomeHolds;
    private final UserRepository users;
    private final PlatformServiceFeePolicy serviceFeePolicy;
    private final PlatformFeeRecordRepository platformFeeRecords;
    private final VerificationCodeService verificationCodes;
    private final AppTestLoginAccountService appTestAccounts;
    private final SecurityEventService securityEvents;

    public WalletView get(Long userId) {
        WalletAccount wallet = wallets.findByUserId(userId)
            .orElseThrow(() -> BusinessException.notFound("账户不存在"));
        ensureSources(wallet);
        return new WalletView(
            wallet.getAvailableBalance(),
            wallet.getFrozenBalance(),
            wallet.getRechargeBalance(),
            wallet.getIncomeBalance(),
            wallet.getPendingIncomeBalance(),
            wallet.getTotalWithdrawn()
        );
    }

    public List<TransactionView> transactions(Long userId) {
        return transactions.findByUserIdOrderByCreatedAtDesc(userId).stream().map(TransactionView::of).toList();
    }

    public AlipayAccountView alipayAccount(Long userId) {
        return alipayAccounts.findByUserId(userId)
            .filter(account -> "OAUTH".equals(account.getAuthorizationType()))
            .map(AlipayAccountView::of)
            .orElse(null);
    }

    public void sendStepUpCode(Long userId, String purpose, String ip, String deviceId) {
        User user = user(userId);
        if (isTest(user)) {
            appTestAccounts.activeVerificationCode(user.getPhone())
                .orElseThrow(() -> BusinessException.forbidden("测试账号已停用"));
            return;
        }
        verificationCodes.send(user.getPhone(), purpose, ip, deviceId);
    }

    @Transactional
    public WithdrawalView withdraw(
        Long userId,
        BigDecimal amount,
        String requestId,
        String verificationCode,
        String ip,
        String deviceId
    ) {
        amount = withdrawalAmount(amount);
        String normalizedRequestId = requireRequestId(requestId);
        Withdrawal existing = withdrawals.findByUserIdAndRequestNo(userId, normalizedRequestId).orElse(null);
        if (existing != null) {
            if (!MoneyAmounts.same(existing.getAmount(), amount)) {
                throw BusinessException.badRequest("重复提现请求的金额不一致");
            }
            return WithdrawalView.of(existing);
        }

        User user = user(userId);
        AlipayAccount payoutAccount = alipayAccounts.findByUserId(userId)
            .filter(account -> "OAUTH".equals(account.getAuthorizationType()))
            .orElseThrow(() -> BusinessException.badRequest("请先完成支付宝授权"));
        if (!isTest(user) && (payoutAccount.getUpdatedAt() == null
            || payoutAccount.getUpdatedAt().plusHours(PAYOUT_ACCOUNT_COOLDOWN_HOURS).isAfter(LocalDateTime.now()))) {
            throw BusinessException.badRequest("支付宝账户授权或变更后24小时内暂不能提现");
        }
        verifyStepUpCode(user, "WITHDRAWAL", verificationCode);

        BigDecimal today = MoneyAmounts.normalize(withdrawals.sumAmountByUserIdAndCreatedAtAfter(
            userId,
            LocalDateTime.now().toLocalDate().atStartOfDay()
        ));
        if (MoneyAmounts.add(today, amount).compareTo(DAILY_WITHDRAWAL_LIMIT) > 0) {
            securityEvents.recordSafely(userId, null, "WITHDRAWAL_DAILY_LIMIT", "HIGH", ip, deviceId, null);
            throw BusinessException.badRequest("今日提现金额已达上限");
        }

        WalletAccount wallet = lock(userId);
        ensureSources(wallet);
        if (wallet.getIncomeBalance().compareTo(amount) < 0) {
            throw BusinessException.badRequest("可提现收入不足");
        }
        wallet.setIncomeBalance(MoneyAmounts.subtract(wallet.getIncomeBalance(), amount));
        syncTotals(wallet);
        wallet.setTotalWithdrawn(MoneyAmounts.add(wallet.getTotalWithdrawn(), amount));

        Withdrawal item = new Withdrawal();
        item.setUser(user);
        item.setAlipayAccount(payoutAccount);
        item.setRequestNo(normalizedRequestId);
        item.setAmount(amount);
        item.setFee(MoneyAmounts.ZERO);
        item.setArrivalAmount(amount);
        item.setPayeeNameSnapshot(payoutAccount.getRealName());
        item.setAlipayIdentifierTypeSnapshot(payoutAccount.getIdentifierType());
        item.setAlipayAccountCiphertextSnapshot(payoutAccount.getAccountCiphertext());
        item.setAlipayAccountMaskedSnapshot(payoutAccount.getAccountMasked());
        item.setStatus(isTest(user) ? "COMPLETED" : "PROCESSING");
        if (isTest(user)) item.setCompletedAt(LocalDateTime.now());
        item = withdrawals.save(item);
        record(wallet, "WITHDRAWAL", "OUT", amount, "WITHDRAWAL", item.getId(), "提现到支付宝");
        securityEvents.recordSafely(
            userId, null, isTest(user) ? "TEST_WITHDRAWAL_COMPLETED" : "WITHDRAWAL_CREATED",
            isTest(user) ? "MEDIUM" : "CRITICAL", ip, deviceId,
            "withdrawalId=" + item.getId() + ", amount=" + amount.toPlainString()
        );
        return WithdrawalView.of(item);
    }

    public List<WithdrawalView> withdrawals(Long userId) {
        return withdrawals.findByUserIdOrderByCreatedAtDesc(userId).stream().map(WithdrawalView::of).toList();
    }

    public PlatformServiceFeePolicy.SettlementQuote quoteInquirySettlement(
        BigDecimal amount,
        String clientPlatform
    ) {
        return serviceFeePolicy.quote(amount, clientPlatform);
    }

    @Transactional
    public FrozenAllocation freeze(Long userId, BigDecimal amount, Long referenceId) {
        WalletAccount wallet = lock(userId);
        ensureSources(wallet);
        amount = MoneyAmounts.requirePositive(amount);
        if (alreadyRecorded(wallet, "INQUIRY_FREEZE", "INQUIRY", referenceId, amount)) {
            BigDecimal recharge = min(wallet.getFrozenRechargeBalance(), amount);
            return new FrozenAllocation(recharge, MoneyAmounts.subtract(amount, recharge));
        }
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw BusinessException.badRequest("余额不足，请先充值");
        }
        BigDecimal rechargeAmount = min(wallet.getRechargeBalance(), amount);
        BigDecimal incomeAmount = MoneyAmounts.subtract(amount, rechargeAmount);
        wallet.setRechargeBalance(MoneyAmounts.subtract(wallet.getRechargeBalance(), rechargeAmount));
        wallet.setIncomeBalance(MoneyAmounts.subtract(wallet.getIncomeBalance(), incomeAmount));
        wallet.setFrozenRechargeBalance(MoneyAmounts.add(wallet.getFrozenRechargeBalance(), rechargeAmount));
        wallet.setFrozenIncomeBalance(MoneyAmounts.add(wallet.getFrozenIncomeBalance(), incomeAmount));
        syncTotals(wallet);
        record(wallet, "INQUIRY_FREEZE", "FREEZE", amount, "INQUIRY", referenceId, "询问金额冻结");
        return new FrozenAllocation(rechargeAmount, incomeAmount);
    }

    @Transactional
    public void refund(Long userId, BigDecimal rechargeAmount, BigDecimal incomeAmount, Long referenceId) {
        WalletAccount wallet = lock(userId);
        ensureSources(wallet);
        BigDecimal total = MoneyAmounts.add(rechargeAmount, incomeAmount);
        if (alreadyRecorded(wallet, "INQUIRY_REFUND", "INQUIRY", referenceId, total)) return;
        ensureSourceFrozen(wallet, rechargeAmount, incomeAmount);
        wallet.setFrozenRechargeBalance(MoneyAmounts.subtract(wallet.getFrozenRechargeBalance(), rechargeAmount));
        wallet.setFrozenIncomeBalance(MoneyAmounts.subtract(wallet.getFrozenIncomeBalance(), incomeAmount));
        wallet.setRechargeBalance(MoneyAmounts.add(wallet.getRechargeBalance(), rechargeAmount));
        wallet.setIncomeBalance(MoneyAmounts.add(wallet.getIncomeBalance(), incomeAmount));
        syncTotals(wallet);
        record(wallet, "INQUIRY_REFUND", "IN", total, "INQUIRY", referenceId, "询问金额退回");
    }

    @Transactional
    public void settle(
        Long questionerId,
        Long answererId,
        BigDecimal rechargeAmount,
        BigDecimal incomeAmount,
        Inquiry inquiry
    ) {
        BigDecimal amount = MoneyAmounts.add(rechargeAmount, incomeAmount);
        BigDecimal serviceFee = MoneyAmounts.normalize(inquiry.getServiceFeeAmount());
        BigDecimal answererIncome = MoneyAmounts.normalize(inquiry.getAnswererIncomeAmount());
        if (!MoneyAmounts.same(MoneyAmounts.add(serviceFee, answererIncome), amount)) {
            throw BusinessException.badRequest("该询问的服务费结算数据不完整，请联系平台处理");
        }
        WalletPair pair = lockPair(questionerId, answererId);
        WalletAccount payer = pair.forUser(questionerId);
        WalletAccount receiver = pair.forUser(answererId);
        ensureSources(payer);
        ensureSources(receiver);
        if (isTest(payer.getUser()) != isTest(receiver.getUser())) {
            throw BusinessException.forbidden("测试资金与真实资金不能互相结算");
        }
        String receiverTransactionType = isTest(receiver.getUser())
            ? "TEST_INQUIRY_INCOME"
            : "INQUIRY_INCOME_PENDING";
        boolean payerRecorded = alreadyRecorded(payer, "INQUIRY_PAYMENT", "INQUIRY", inquiry.getId(), amount);
        boolean receiverRecorded = alreadyRecorded(
            receiver, receiverTransactionType, "INQUIRY", inquiry.getId(), answererIncome
        );
        if (payerRecorded && receiverRecorded) {
            savePlatformFeeRecord(inquiry, amount, serviceFee, answererIncome);
            return;
        }
        if (payerRecorded || receiverRecorded) {
            throw BusinessException.badRequest("该询问的资金流水不完整，请联系平台处理");
        }
        ensureSourceFrozen(payer, rechargeAmount, incomeAmount);
        payer.setFrozenRechargeBalance(MoneyAmounts.subtract(payer.getFrozenRechargeBalance(), rechargeAmount));
        payer.setFrozenIncomeBalance(MoneyAmounts.subtract(payer.getFrozenIncomeBalance(), incomeAmount));
        syncTotals(payer);
        record(payer, "INQUIRY_PAYMENT", "OUT", amount, "INQUIRY", inquiry.getId(), "询问支出");

        if (isTest(receiver.getUser())) {
            receiver.setIncomeBalance(MoneyAmounts.add(receiver.getIncomeBalance(), answererIncome));
            syncTotals(receiver);
            record(
                receiver, "TEST_INQUIRY_INCOME", "IN", answererIncome,
                "INQUIRY", inquiry.getId(), "测试询问净收入"
            );
            savePlatformFeeRecord(inquiry, amount, serviceFee, answererIncome);
            return;
        }

        receiver.setPendingIncomeBalance(MoneyAmounts.add(receiver.getPendingIncomeBalance(), answererIncome));
        record(
            receiver, "INQUIRY_INCOME_PENDING", "HOLD", answererIncome,
            "INQUIRY", inquiry.getId(), "回答净收入待解冻"
        );
        WalletIncomeHold hold = new WalletIncomeHold();
        hold.setUser(receiver.getUser());
        hold.setInquiry(inquiry);
        hold.setAmount(answererIncome);
        hold.setReleaseAt(LocalDateTime.now().plusHours(24));
        incomeHolds.save(hold);
        savePlatformFeeRecord(inquiry, amount, serviceFee, answererIncome);
    }

    private void savePlatformFeeRecord(
        Inquiry inquiry,
        BigDecimal grossAmount,
        BigDecimal serviceFeeAmount,
        BigDecimal answererIncomeAmount
    ) {
        if (platformFeeRecords.existsByInquiryId(inquiry.getId())) return;
        PlatformFeeRecord record = new PlatformFeeRecord();
        record.setInquiry(inquiry);
        record.setClientPlatform(inquiry.getClientPlatform());
        record.setGrossAmount(grossAmount);
        record.setServiceFeeRate(inquiry.getServiceFeeRate());
        record.setServiceFeeAmount(serviceFeeAmount);
        record.setAnswererIncomeAmount(answererIncomeAmount);
        platformFeeRecords.save(record);
    }

    @Scheduled(fixedDelayString = "${app.wallet.income-release-scan-ms:60000}")
    @Transactional
    public void releaseMatureIncome() {
        incomeHolds.findTop100ByStatusAndReleaseAtBeforeOrderByReleaseAtAsc("PENDING", LocalDateTime.now())
            .forEach(candidate -> {
                WalletIncomeHold hold = incomeHolds.findWithLockById(candidate.getId()).orElse(null);
                if (hold == null || !"PENDING".equals(hold.getStatus())) return;
                WalletAccount wallet = lock(hold.getUser().getId());
                ensureSources(wallet);
                if (wallet.getPendingIncomeBalance().compareTo(hold.getAmount()) < 0) {
                    securityEvents.recordSafely(
                        wallet.getUser().getId(), null, "INCOME_HOLD_MISMATCH", "CRITICAL", null, null,
                        "holdId=" + hold.getId()
                    );
                    return;
                }
                wallet.setPendingIncomeBalance(MoneyAmounts.subtract(wallet.getPendingIncomeBalance(), hold.getAmount()));
                wallet.setIncomeBalance(MoneyAmounts.add(wallet.getIncomeBalance(), hold.getAmount()));
                syncTotals(wallet);
                hold.setStatus("RELEASED");
                hold.setReleasedAt(LocalDateTime.now());
                record(
                    wallet, "INQUIRY_INCOME_RELEASE", "IN", hold.getAmount(), "INQUIRY",
                    hold.getInquiry().getId(), "回答收入已解冻"
                );
            });
    }

    @Transactional
    public void creditRecharge(Long userId, BigDecimal amount, Long referenceId) {
        User user = user(userId);
        WalletAccount wallet = lock(user.getId());
        ensureSources(wallet);
        amount = MoneyAmounts.requirePositive(amount);
        String transactionType = isTest(user) ? "TEST_RECHARGE" : "RECHARGE";
        if (alreadyRecorded(wallet, transactionType, "RECHARGE", referenceId, amount)) return;
        wallet.setRechargeBalance(MoneyAmounts.add(wallet.getRechargeBalance(), amount));
        syncTotals(wallet);
        record(
            wallet, transactionType, "IN", amount, "RECHARGE", referenceId,
            isTest(user) ? "测试余额充值" : "支付宝充值"
        );
    }

    private User user(Long userId) {
        return users.findById(userId).orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }

    private boolean isTest(User user) {
        return "TEST".equals(user.getAccountType());
    }

    private void verifyStepUpCode(User user, String purpose, String code) {
        if (!isTest(user)) {
            verificationCodes.verify(user.getPhone(), purpose, code);
            return;
        }
        String expected = appTestAccounts.activeVerificationCode(user.getPhone())
            .orElseThrow(() -> BusinessException.forbidden("测试账号已停用"));
        if (!java.security.MessageDigest.isEqual(
            expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            (code == null ? "" : code).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        )) {
            throw BusinessException.badRequest("验证码不正确");
        }
    }

    private WalletAccount lock(Long userId) {
        return wallets.findWithLockByUserId(userId).orElseThrow(() -> BusinessException.notFound("账户不存在"));
    }

    private void ensureSourceFrozen(WalletAccount wallet, BigDecimal rechargeAmount, BigDecimal incomeAmount) {
        if (wallet.getFrozenRechargeBalance().compareTo(rechargeAmount) < 0
            || wallet.getFrozenIncomeBalance().compareTo(incomeAmount) < 0) {
            throw BusinessException.badRequest("冻结金额异常");
        }
    }

    private WalletPair lockPair(Long firstUserId, Long secondUserId) {
        if (firstUserId.equals(secondUserId)) throw BusinessException.badRequest("付款人与收款人不能相同");
        Long lowerId = Math.min(firstUserId, secondUserId);
        Long higherId = Math.max(firstUserId, secondUserId);
        WalletAccount lower = lock(lowerId);
        WalletAccount higher = lock(higherId);
        return new WalletPair(lower, higher);
    }

    private BigDecimal withdrawalAmount(BigDecimal amount) {
        return MoneyAmounts.requireWholeAmount(amount, BigDecimal.ONE, new BigDecimal("9999"), "提现金额");
    }

    private String requireRequestId(String requestId) {
        String value = requestId == null ? "" : requestId.trim();
        if (!value.matches("[A-Za-z0-9_-]{12,64}")) throw BusinessException.badRequest("提现请求标识无效");
        return value;
    }

    private boolean alreadyRecorded(
        WalletAccount wallet,
        String type,
        String referenceType,
        Long referenceId,
        BigDecimal amount
    ) {
        if (referenceId == null) return false;
        return transactions.findByUserIdAndTransactionTypeAndReferenceTypeAndReferenceId(
            wallet.getUser().getId(), type, referenceType, referenceId
        ).map(existing -> {
            if (!MoneyAmounts.same(existing.getAmount(), amount)) {
                throw BusinessException.badRequest("重复资金操作的金额不一致");
            }
            return true;
        }).orElse(false);
    }

    private void record(
        WalletAccount wallet,
        String type,
        String direction,
        BigDecimal amount,
        String referenceType,
        Long referenceId,
        String description
    ) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUser(wallet.getUser());
        tx.setTransactionType(type);
        tx.setDirection(direction);
        tx.setAmount(amount);
        tx.setAvailableAfter(wallet.getAvailableBalance());
        tx.setFrozenAfter(wallet.getFrozenBalance());
        tx.setReferenceType(referenceType);
        tx.setReferenceId(referenceId);
        tx.setDescription(description);
        transactions.save(tx);
    }

    private void ensureSources(WalletAccount wallet) {
        BigDecimal availableSources = MoneyAmounts.add(wallet.getRechargeBalance(), wallet.getIncomeBalance());
        if (!MoneyAmounts.same(availableSources, wallet.getAvailableBalance())) {
            BigDecimal difference = MoneyAmounts.subtract(wallet.getAvailableBalance(), availableSources);
            wallet.setRechargeBalance(MoneyAmounts.add(wallet.getRechargeBalance(), difference));
        }
        BigDecimal frozenSources = MoneyAmounts.add(
            wallet.getFrozenRechargeBalance(),
            wallet.getFrozenIncomeBalance()
        );
        if (!MoneyAmounts.same(frozenSources, wallet.getFrozenBalance())) {
            BigDecimal difference = MoneyAmounts.subtract(wallet.getFrozenBalance(), frozenSources);
            wallet.setFrozenRechargeBalance(MoneyAmounts.add(wallet.getFrozenRechargeBalance(), difference));
        }
        syncTotals(wallet);
    }

    private void syncTotals(WalletAccount wallet) {
        wallet.setAvailableBalance(MoneyAmounts.add(wallet.getRechargeBalance(), wallet.getIncomeBalance()));
        wallet.setFrozenBalance(MoneyAmounts.add(
            wallet.getFrozenRechargeBalance(),
            wallet.getFrozenIncomeBalance()
        ));
    }

    private BigDecimal min(BigDecimal first, BigDecimal second) {
        return first.compareTo(second) <= 0 ? MoneyAmounts.normalize(first) : MoneyAmounts.normalize(second);
    }

    public record FrozenAllocation(BigDecimal rechargeAmount, BigDecimal incomeAmount) {
    }

    public record WalletView(
        BigDecimal availableBalance,
        BigDecimal frozenBalance,
        BigDecimal rechargeBalance,
        BigDecimal withdrawableIncome,
        BigDecimal pendingIncome,
        BigDecimal totalWithdrawn
    ) {
    }

    public record TransactionView(
        Long id,
        String type,
        String direction,
        BigDecimal amount,
        BigDecimal availableAfter,
        BigDecimal frozenAfter,
        String description,
        LocalDateTime createdAt
    ) {
        static TransactionView of(WalletTransaction item) {
            return new TransactionView(
                item.getId(), item.getTransactionType(), item.getDirection(), item.getAmount(),
                item.getAvailableAfter(), item.getFrozenAfter(), item.getDescription(), item.getCreatedAt()
            );
        }
    }

    public record AlipayAccountView(
        Long id,
        String displayName,
        String identifierType,
        String accountMasked,
        LocalDateTime authorizedAt
    ) {
        static AlipayAccountView of(AlipayAccount account) {
            return new AlipayAccountView(
                account.getId(), account.getDisplayName(), account.getIdentifierType(),
                account.getAccountMasked(), account.getAuthorizedAt()
            );
        }
    }

    public record WithdrawalView(
        Long id,
        BigDecimal amount,
        String payeeName,
        String alipayAccount,
        String status,
        String batchNo,
        LocalDateTime exportedAt,
        LocalDateTime createdAt
    ) {
        static WithdrawalView of(Withdrawal item) {
            return new WithdrawalView(
                item.getId(), item.getAmount(),
                item.getPayeeNameSnapshot(), item.getAlipayAccountMaskedSnapshot(), item.getStatus(),
                item.getBatchNo(), item.getExportedAt(), item.getCreatedAt()
            );
        }
    }

    private record WalletPair(WalletAccount lower, WalletAccount higher) {
        WalletAccount forUser(Long userId) {
            if (lower.getUser().getId().equals(userId)) return lower;
            if (higher.getUser().getId().equals(userId)) return higher;
            throw BusinessException.notFound("账户不存在");
        }
    }
}
