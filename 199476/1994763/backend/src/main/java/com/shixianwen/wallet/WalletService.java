package com.shixianwen.wallet;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.auth.VerificationCodeService;
import com.shixianwen.auth.AppTestLoginAccountService;
import com.shixianwen.common.BusinessException;
import com.shixianwen.finance.FinancialLedgerService;
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

import static com.shixianwen.finance.FinancialLedgerService.entry;
import static com.shixianwen.finance.FinancialLedgerService.negative;

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
    private final AnalyticsEventService analytics;
    private final FinancialLedgerService ledger;

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
        ledger.record(
            "WITHDRAWAL", item.getId(), "REQUEST", "提现申请",
            List.of(
                entry("USER_INCOME_LIABILITY", userId, amount),
                entry("WITHDRAWAL_PAYABLE", userId, negative(amount))
            )
        );
        if (isTest(user)) {
            ledger.record(
                "WITHDRAWAL", item.getId(), "SUCCESS", "测试提现完成",
                List.of(
                    entry("WITHDRAWAL_PAYABLE", userId, amount),
                    entry("TEST_CLEARING", null, negative(amount))
                )
            );
        }
        securityEvents.recordSafely(
            userId, null, isTest(user) ? "TEST_WITHDRAWAL_COMPLETED" : "WITHDRAWAL_CREATED",
            isTest(user) ? "MEDIUM" : "CRITICAL", ip, deviceId,
            "withdrawalId=" + item.getId() + ", amount=" + amount.toPlainString()
        );
        analytics.recordBusinessAfterCommit(user, "withdrawal_submit", java.util.Map.of(
            "withdrawal_id", item.getId(),
            "test_account", isTest(user),
            "amount_bucket", withdrawalAmountBucket(amount)
        ));
        return WithdrawalView.of(item);
    }

    public List<WithdrawalView> withdrawals(Long userId) {
        return withdrawals.findByUserIdOrderByCreatedAtDesc(userId).stream().map(WithdrawalView::of).toList();
    }

    private String withdrawalAmountBucket(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("100")) <= 0) return "1-100";
        if (amount.compareTo(new BigDecimal("500")) <= 0) return "101-500";
        if (amount.compareTo(new BigDecimal("2000")) <= 0) return "501-2000";
        return "2001-9999";
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
        ledger.record(
            "INQUIRY", referenceId, "FREEZE", "询问金额冻结",
            List.of(
                entry("USER_RECHARGE_LIABILITY", userId, rechargeAmount),
                entry("USER_INCOME_LIABILITY", userId, incomeAmount),
                entry("INQUIRY_FROZEN", null, negative(amount))
            )
        );
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
        ledger.record(
            "INQUIRY", referenceId, "REFUND", "询问金额退回",
            List.of(
                entry("INQUIRY_FROZEN", null, total),
                entry("USER_RECHARGE_LIABILITY", userId, negative(rechargeAmount)),
                entry("USER_INCOME_LIABILITY", userId, negative(incomeAmount))
            )
        );
    }

    @Transactional
    public void refundInquiryTimeout(
        Long userId,
        BigDecimal rechargeAmount,
        BigDecimal incomeAmount,
        Long inquiryId,
        int timeoutNo
    ) {
        if (timeoutNo < 1 || timeoutNo > 5) {
            throw BusinessException.badRequest("询问超时次数不正确");
        }
        WalletAccount wallet = lock(userId);
        ensureSources(wallet);
        BigDecimal total = MoneyAmounts.add(rechargeAmount, incomeAmount);
        String transactionType = "INQUIRY_TIMEOUT_REFUND_" + timeoutNo;
        if (alreadyRecorded(wallet, transactionType, "INQUIRY", inquiryId, total)) return;
        ensureSourceFrozen(wallet, rechargeAmount, incomeAmount);
        wallet.setFrozenRechargeBalance(MoneyAmounts.subtract(wallet.getFrozenRechargeBalance(), rechargeAmount));
        wallet.setFrozenIncomeBalance(MoneyAmounts.subtract(wallet.getFrozenIncomeBalance(), incomeAmount));
        wallet.setRechargeBalance(MoneyAmounts.add(wallet.getRechargeBalance(), rechargeAmount));
        wallet.setIncomeBalance(MoneyAmounts.add(wallet.getIncomeBalance(), incomeAmount));
        syncTotals(wallet);
        record(
            wallet,
            transactionType,
            "IN",
            total,
            "INQUIRY",
            inquiryId,
            "回答超时退款（第" + timeoutNo + "次）"
        );
        ledger.record(
            "INQUIRY",
            inquiryId,
            "TIMEOUT_REFUND_" + timeoutNo,
            "回答超时退回20%询问金额",
            List.of(
                entry("INQUIRY_FROZEN", null, total),
                entry("USER_RECHARGE_LIABILITY", userId, negative(rechargeAmount)),
                entry("USER_INCOME_LIABILITY", userId, negative(incomeAmount))
            )
        );
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
            savePlatformFeeRecord(
                inquiry,
                amount,
                serviceFee,
                answererIncome,
                isTest(receiver.getUser()) ? "EARNED" : "PENDING"
            );
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
            savePlatformFeeRecord(inquiry, amount, serviceFee, answererIncome, "EARNED");
            ledger.record(
                "INQUIRY", inquiry.getId(), "SETTLE", "测试询问结算",
                List.of(
                    entry("INQUIRY_FROZEN", null, amount),
                    entry("USER_INCOME_LIABILITY", answererId, negative(answererIncome)),
                    entry("PLATFORM_SERVICE_FEE", null, negative(serviceFee))
                )
            );
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
        savePlatformFeeRecord(inquiry, amount, serviceFee, answererIncome, "PENDING");
        ledger.record(
            "INQUIRY", inquiry.getId(), "SETTLE", "询问结算待解冻",
            List.of(
                entry("INQUIRY_FROZEN", null, amount),
                entry("ANSWERER_PENDING", answererId, negative(answererIncome)),
                entry("PLATFORM_FEE_PENDING", null, negative(serviceFee))
            )
        );
    }

    private void savePlatformFeeRecord(
        Inquiry inquiry,
        BigDecimal grossAmount,
        BigDecimal serviceFeeAmount,
        BigDecimal answererIncomeAmount,
        String status
    ) {
        if (platformFeeRecords.existsByInquiryId(inquiry.getId())) return;
        PlatformFeeRecord record = new PlatformFeeRecord();
        record.setInquiry(inquiry);
        record.setClientPlatform(inquiry.getClientPlatform());
        record.setGrossAmount(grossAmount);
        record.setServiceFeeRate(inquiry.getServiceFeeRate());
        record.setServiceFeeAmount(serviceFeeAmount);
        record.setAnswererIncomeAmount(answererIncomeAmount);
        record.setStatus(status);
        if ("EARNED".equals(status)) record.setFinalizedAt(LocalDateTime.now());
        platformFeeRecords.save(record);
    }

    @Scheduled(fixedDelayString = "${app.wallet.income-release-scan-ms:60000}")
    @Transactional
    public void releaseMatureIncome() {
        incomeHolds.findTop100ByStatusAndReleaseAtBeforeOrderByReleaseAtAsc("PENDING", LocalDateTime.now())
            .forEach(candidate -> {
                WalletIncomeHold hold = incomeHolds.findWithLockById(candidate.getId()).orElse(null);
                if (hold == null || !"PENDING".equals(hold.getStatus())) return;
                releaseHold(hold);
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
        ledger.record(
            "RECHARGE", referenceId, "PAID", isTest(user) ? "测试充值到账" : "支付宝充值到账",
            List.of(
                entry(isTest(user) ? "TEST_CLEARING" : "ALIPAY_CLEARING", null, amount),
                entry("USER_RECHARGE_LIABILITY", userId, negative(amount))
            )
        );
    }

    @Transactional
    public void creditInvitationReward(Long userId, BigDecimal amount, Long invitationId) {
        WalletAccount wallet = lock(userId);
        ensureSources(wallet);
        amount = MoneyAmounts.requirePositive(amount);
        if (alreadyRecorded(
            wallet,
            "INVITATION_REWARD",
            "USER_INVITATION",
            invitationId,
            amount
        )) {
            return;
        }
        wallet.setIncomeBalance(MoneyAmounts.add(wallet.getIncomeBalance(), amount));
        syncTotals(wallet);
        record(
            wallet,
            "INVITATION_REWARD",
            "IN",
            amount,
            "USER_INVITATION",
            invitationId,
            "邀请答主红包"
        );
        ledger.record(
            "USER_INVITATION", invitationId, "REWARD", "邀请答主红包",
            List.of(
                entry("MARKETING_EXPENSE", null, amount),
                entry("USER_INCOME_LIABILITY", userId, negative(amount))
            )
        );
    }

    @Transactional
    public void holdForQualityReview(Long inquiryId) {
        WalletIncomeHold hold = incomeHolds.findWithLockByInquiryId(inquiryId)
            .orElseThrow(() -> BusinessException.badRequest("该询问没有待解冻收入"));
        if (!"PENDING".equals(hold.getStatus()) || !hold.getReleaseAt().isAfter(LocalDateTime.now())) {
            throw BusinessException.badRequest("该笔收入已解冻，不能再申请资金复核");
        }
        hold.setStatus("DISPUTED");
    }

    @Transactional
    public void resolveQualitySettlement(Inquiry inquiry) {
        WalletIncomeHold hold = incomeHolds.findWithLockByInquiryId(inquiry.getId())
            .orElseThrow(() -> BusinessException.badRequest("待解冻收入不存在"));
        if (!"DISPUTED".equals(hold.getStatus())) {
            throw BusinessException.badRequest("该笔收入不在质量复核中");
        }
        releaseHold(hold);
    }

    @Transactional
    public void resolveQualityRefund(Inquiry inquiry) {
        WalletIncomeHold hold = incomeHolds.findWithLockByInquiryId(inquiry.getId())
            .orElseThrow(() -> BusinessException.badRequest("待解冻收入不存在"));
        if (!"DISPUTED".equals(hold.getStatus())) {
            throw BusinessException.badRequest("该笔收入不在质量复核中");
        }
        WalletPair pair = lockPair(inquiry.getQuestioner().getId(), inquiry.getAnswerer().getId());
        WalletAccount questioner = pair.forUser(inquiry.getQuestioner().getId());
        WalletAccount answerer = pair.forUser(inquiry.getAnswerer().getId());
        ensureSources(questioner);
        ensureSources(answerer);
        if (answerer.getPendingIncomeBalance().compareTo(hold.getAmount()) < 0) {
            throw BusinessException.badRequest("回答收入冻结金额异常");
        }
        answerer.setPendingIncomeBalance(MoneyAmounts.subtract(answerer.getPendingIncomeBalance(), hold.getAmount()));
        syncTotals(answerer);
        questioner.setRechargeBalance(MoneyAmounts.add(questioner.getRechargeBalance(), inquiry.getFrozenRechargeAmount()));
        questioner.setIncomeBalance(MoneyAmounts.add(questioner.getIncomeBalance(), inquiry.getFrozenIncomeAmount()));
        syncTotals(questioner);
        hold.setStatus("REFUNDED");
        hold.setReleasedAt(LocalDateTime.now());
        record(answerer, "INQUIRY_INCOME_REVERSED", "OUT", hold.getAmount(), "INQUIRY", inquiry.getId(), "质量复核退款，回答收入撤回");
        record(questioner, "INQUIRY_QUALITY_REFUND", "IN", inquiry.getAmount(), "INQUIRY", inquiry.getId(), "质量复核退款");
        platformFeeRecords.findByInquiryId(inquiry.getId()).ifPresent(record -> {
            record.setStatus("REVERSED");
            record.setFinalizedAt(LocalDateTime.now());
        });
        ledger.record(
            "INQUIRY", inquiry.getId(), "QUALITY_REFUND", "质量复核全额退款",
            List.of(
                entry("ANSWERER_PENDING", inquiry.getAnswerer().getId(), hold.getAmount()),
                entry("PLATFORM_FEE_PENDING", null, inquiry.getServiceFeeAmount()),
                entry("USER_RECHARGE_LIABILITY", inquiry.getQuestioner().getId(), negative(inquiry.getFrozenRechargeAmount())),
                entry("USER_INCOME_LIABILITY", inquiry.getQuestioner().getId(), negative(inquiry.getFrozenIncomeAmount()))
            )
        );
    }

    private void releaseHold(WalletIncomeHold hold) {
        WalletAccount wallet = lock(hold.getUser().getId());
        ensureSources(wallet);
        if (wallet.getPendingIncomeBalance().compareTo(hold.getAmount()) < 0) {
            securityEvents.recordSafely(
                wallet.getUser().getId(), null, "INCOME_HOLD_MISMATCH", "CRITICAL", null, null,
                "holdId=" + hold.getId()
            );
            throw BusinessException.badRequest("待解冻收入金额异常");
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
        PlatformFeeRecord fee = platformFeeRecords.findByInquiryId(hold.getInquiry().getId()).orElse(null);
        if (fee != null && "PENDING".equals(fee.getStatus())) {
            fee.setStatus("EARNED");
            fee.setFinalizedAt(LocalDateTime.now());
        }
        BigDecimal serviceFee = fee == null ? hold.getInquiry().getServiceFeeAmount() : fee.getServiceFeeAmount();
        ledger.record(
            "INQUIRY", hold.getInquiry().getId(), "RELEASE", "回答收入解冻",
            List.of(
                entry("ANSWERER_PENDING", hold.getUser().getId(), hold.getAmount()),
                entry("PLATFORM_FEE_PENDING", null, serviceFee),
                entry("USER_INCOME_LIABILITY", hold.getUser().getId(), negative(hold.getAmount())),
                entry("PLATFORM_SERVICE_FEE", null, negative(serviceFee))
            )
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
