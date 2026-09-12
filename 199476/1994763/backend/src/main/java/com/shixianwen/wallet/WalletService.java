package com.shixianwen.wallet;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.auth.VerificationCodeService;
import com.shixianwen.auth.AppTestLoginAccountService;
import com.shixianwen.common.BusinessException;
import com.shixianwen.config.AppGlobalSettingService;
import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.finance.FinancialLedgerService;
import com.shixianwen.inquiry.Inquiry;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

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
    private final ExperienceTipRepository experienceTips;
    private final CertificationRepository certifications;
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
    private final JdbcTemplate jdbc;
    @Autowired(required = false)
    private AppGlobalSettingService globalSettings;

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

    @Transactional
    public void recordCuratedMembershipPurchase(
        Long userId,
        Long orderId,
        BigDecimal amount,
        boolean testPayment
    ) {
        BigDecimal normalizedAmount = MoneyAmounts.requirePositive(amount);
        WalletAccount wallet = wallets.findWithLockByUserId(userId)
            .orElseThrow(() -> BusinessException.notFound("账户不存在"));
        ensureSources(wallet);
        if (alreadyRecorded(
            wallet,
            "CURATED_MEMBERSHIP_PURCHASE",
            "CURATED_MEMBERSHIP",
            orderId,
            normalizedAmount
        )) return;
        record(
            wallet,
            "CURATED_MEMBERSHIP_PURCHASE",
            "OUT",
            normalizedAmount,
            "CURATED_MEMBERSHIP",
            orderId,
            testPayment ? "测试开通严选直聊" : "开通严选直聊"
        );
        ledger.record(
            "CURATED_MEMBERSHIP",
            orderId,
            "PAID",
            testPayment ? "测试开通严选直聊" : "严选直聊开通到账",
            List.of(
                entry(testPayment ? "TEST_CLEARING" : "ALIPAY_CLEARING", null, normalizedAmount),
                entry(
                    testPayment ? "TEST_MEMBERSHIP_REVENUE" : "PLATFORM_MEMBERSHIP_REVENUE",
                    null,
                    negative(normalizedAmount)
                )
            )
        );
    }

    public AlipayAccountView alipayAccount(Long userId) {
        return alipayAccounts.findByUserId(userId)
            .filter(account -> "OAUTH".equals(account.getAuthorizationType()))
            .map(AlipayAccountView::of)
            .orElse(null);
    }

    public void sendStepUpCode(Long userId, String purpose, String ip, String deviceId) {
        User user = user(userId);
        if ("WITHDRAWAL".equalsIgnoreCase(purpose)) {
            requireWithdrawalIdentity(userId);
        }
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
        AppGlobalSettingService.Settings settings = settings();
        if (settings != null && !settings.withdrawalEnabled()) {
            throw BusinessException.serviceUnavailable("提现功能暂时不可用");
        }
        amount = withdrawalAmount(amount, settings);
        String normalizedRequestId = requireRequestId(requestId);
        Withdrawal existing = withdrawals.findByUserIdAndRequestNo(userId, normalizedRequestId).orElse(null);
        if (existing != null) {
            if (!MoneyAmounts.same(existing.getAmount(), amount)) {
                throw BusinessException.badRequest("重复提现请求的金额不一致");
            }
            return WithdrawalView.of(existing);
        }

        User user = user(userId);
        requireWithdrawalIdentity(userId);
        AlipayAccount payoutAccount = alipayAccounts.findByUserId(userId)
            .filter(account -> "OAUTH".equals(account.getAuthorizationType()))
            .orElseThrow(() -> BusinessException.badRequest("请先完成支付宝授权"));
        if (!isTest(user) && (payoutAccount.getUpdatedAt() == null
            || payoutAccount.getUpdatedAt().plusHours(settings == null ? PAYOUT_ACCOUNT_COOLDOWN_HOURS : settings.payoutAccountCooldownHours()).isAfter(LocalDateTime.now()))) {
            int hours = settings == null ? (int) PAYOUT_ACCOUNT_COOLDOWN_HOURS : settings.payoutAccountCooldownHours();
            throw BusinessException.badRequest("支付宝账户授权或变更后" + hours + "小时内暂不能提现");
        }
        verifyStepUpCode(user, "WITHDRAWAL", verificationCode);

        BigDecimal today = MoneyAmounts.normalize(withdrawals.sumAmountByUserIdAndCreatedAtAfter(
            userId,
            LocalDateTime.now().toLocalDate().atStartOfDay()
        ));
        BigDecimal dailyLimit = settings == null ? DAILY_WITHDRAWAL_LIMIT : settings.withdrawalDailyLimit();
        if (MoneyAmounts.add(today, amount).compareTo(dailyLimit) > 0) {
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
        WithdrawalRisk withdrawalRisk = withdrawalRisk(user);
        item.setRiskLevel(withdrawalRisk.level());
        item.setRiskReasons(withdrawalRisk.reasons());
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
        if (!"LOW".equals(withdrawalRisk.level())) {
            securityEvents.recordSafely(
                userId, null, "WITHDRAWAL_REVIEW_RISK", withdrawalRisk.level(), ip, deviceId,
                "withdrawalId=" + item.getId() + ", reasons=" + withdrawalRisk.reasons()
            );
        }
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

    private void requireWithdrawalIdentity(Long userId) {
        if (!certifications.existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            userId,
            "IDENTITY",
            "APPROVED"
        )) {
            throw BusinessException.badRequest("完成实名认证后才能提现");
        }
    }

    private String withdrawalAmountBucket(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("100")) <= 0) return "1-100";
        if (amount.compareTo(new BigDecimal("500")) <= 0) return "101-500";
        if (amount.compareTo(new BigDecimal("2000")) <= 0) return "501-2000";
        return "2001-9999";
    }

    private WithdrawalRisk withdrawalRisk(User user) {
        List<String> reasons = new java.util.ArrayList<>();
        Long riskyReward = jdbc.queryForObject(
            "SELECT COUNT(*) FROM first_experience_rewards WHERE user_id=? AND risk_level<>'LOW'",
            Long.class,
            user.getId()
        );
        if (riskyReward != null && riskyReward > 0) reasons.add("首次经历奖励存在关联风险");

        Long relatedTransfers = jdbc.queryForObject(
            "SELECT COUNT(*) FROM (" +
                "SELECT i.id FROM inquiries i JOIN users q ON q.id=i.questioner_id " +
                "WHERE i.answerer_id=? AND (" +
                "(? IS NOT NULL AND ?<>'' AND (q.register_device_id=? OR q.last_login_device_id=?)) OR " +
                "(? IS NOT NULL AND ?<>'' AND q.register_ip=?)) " +
                "UNION ALL " +
                "SELECT t.id FROM experience_tips t JOIN users p ON p.id=t.payer_user_id " +
                "WHERE t.receiver_user_id=? AND (" +
                "(? IS NOT NULL AND ?<>'' AND (p.register_device_id=? OR p.last_login_device_id=?)) OR " +
                "(? IS NOT NULL AND ?<>'' AND p.register_ip=?))" +
                ") related",
            Long.class,
            user.getId(),
            user.getRegisterDeviceId(), user.getRegisterDeviceId(), user.getRegisterDeviceId(), user.getRegisterDeviceId(),
            user.getRegisterIp(), user.getRegisterIp(), user.getRegisterIp(),
            user.getId(),
            user.getRegisterDeviceId(), user.getRegisterDeviceId(), user.getRegisterDeviceId(), user.getRegisterDeviceId(),
            user.getRegisterIp(), user.getRegisterIp(), user.getRegisterIp()
        );
        if (relatedTransfers != null && relatedTransfers > 0) {
            reasons.add("收入来源与本人设备或IP存在关联");
        }
        String level = reasons.isEmpty() ? "LOW" : relatedTransfers != null && relatedTransfers > 0 ? "CRITICAL" : "HIGH";
        return new WithdrawalRisk(level, reasons.isEmpty() ? null : String.join("；", reasons));
    }

    private record WithdrawalRisk(String level, String reasons) {}

    public PlatformServiceFeePolicy.SettlementQuote quoteInquirySettlement(
        BigDecimal amount,
        String clientPlatform
    ) {
        return serviceFeePolicy.quote(amount, clientPlatform);
    }

    public PlatformServiceFeePolicy.SettlementQuote quoteIncomeServiceFee(
        BigDecimal amount,
        String clientPlatform
    ) {
        return serviceFeePolicy.quote(amount, clientPlatform);
    }

    @Transactional
    public ExperienceTipView tipExperience(
        Long payerUserId,
        Long certificationId,
        BigDecimal requestedAmount,
        String requestId
    ) {
        return tipExperience(payerUserId, certificationId, requestedAmount, requestId, "ANDROID");
    }

    @Transactional
    public ExperienceTipView tipExperience(
        Long payerUserId,
        Long certificationId,
        BigDecimal requestedAmount,
        String requestId,
        String clientPlatform
    ) {
        AppGlobalSettingService.Settings settings = settings();
        if (settings != null && !settings.experienceTipEnabled()) {
            throw BusinessException.serviceUnavailable("打赏功能暂时不可用");
        }
        String normalizedRequestId = requireRequestId(requestId, "打赏");
        BigDecimal amount = MoneyAmounts.requireWholeAmount(
            requestedAmount,
            BigDecimal.ONE,
            settings == null ? new BigDecimal("5000") : BigDecimal.valueOf(settings.tipMaxAmount()),
            "打赏金额"
        );

        ExperienceTip existing = experienceTips
            .findByPayerIdAndRequestNo(payerUserId, normalizedRequestId)
            .orElse(null);
        if (existing != null) {
            verifySameTip(existing, certificationId, amount);
            return ExperienceTipView.of(existing);
        }

        Certification certification = certifications.findById(certificationId)
            .orElseThrow(() -> BusinessException.notFound("经历不存在"));
        if (!"EXPERIENCE".equals(certification.getCategory())
            || !"APPROVED".equals(certification.getStatus())
            || !certification.isEnabled()) {
            throw BusinessException.badRequest("这段经历暂不可打赏");
        }
        Long receiverUserId = certification.getUser().getId();
        if (payerUserId.equals(receiverUserId)) {
            throw BusinessException.badRequest("不能打赏自己的经历");
        }
        WalletPair pair = lockPair(payerUserId, receiverUserId);
        WalletAccount payer = pair.forUser(payerUserId);
        WalletAccount receiver = pair.forUser(receiverUserId);
        ensureSources(payer);
        ensureSources(receiver);
        if (isTest(payer.getUser()) != isTest(receiver.getUser())) {
            throw BusinessException.forbidden("测试资金与真实资金不能互相流转");
        }
        recordTransferRisk(payer.getUser(), receiver.getUser(), "EXPERIENCE_TIP", certificationId);

        existing = experienceTips.findByPayerIdAndRequestNo(payerUserId, normalizedRequestId).orElse(null);
        if (existing != null) {
            verifySameTip(existing, certificationId, amount);
            return ExperienceTipView.of(existing);
        }
        if (payer.getAvailableBalance().compareTo(amount) < 0) {
            throw BusinessException.badRequest("余额不足，请先充值");
        }

        BigDecimal rechargeAmount = min(payer.getRechargeBalance(), amount);
        BigDecimal incomeAmount = MoneyAmounts.subtract(amount, rechargeAmount);
        payer.setRechargeBalance(MoneyAmounts.subtract(payer.getRechargeBalance(), rechargeAmount));
        payer.setIncomeBalance(MoneyAmounts.subtract(payer.getIncomeBalance(), incomeAmount));
        var feeQuote = quoteIncomeServiceFee(amount, clientPlatform);
        BigDecimal feeRate = feeQuote.serviceFeeRate();
        BigDecimal feeAmount = feeQuote.serviceFeeAmount();
        BigDecimal receiverIncome = feeQuote.answererIncomeAmount();
        if (isTest(receiver.getUser())) {
            receiver.setIncomeBalance(MoneyAmounts.add(receiver.getIncomeBalance(), receiverIncome));
        } else {
            receiver.setPendingIncomeBalance(MoneyAmounts.add(receiver.getPendingIncomeBalance(), receiverIncome));
        }
        syncTotals(payer);
        syncTotals(receiver);

        ExperienceTip tip = new ExperienceTip();
        tip.setPayer(payer.getUser());
        tip.setReceiver(receiver.getUser());
        tip.setCertification(certification);
        tip.setRequestNo(normalizedRequestId);
        tip.setAmount(amount);
        tip.setFeeRate(feeRate);
        tip.setFeeAmount(feeAmount);
        tip.setReceiverIncomeAmount(receiverIncome);
        tip = experienceTips.save(tip);

        record(payer, "EXPERIENCE_TIP", "OUT", amount, "EXPERIENCE_TIP", tip.getId(), "经历打赏");
        record(
            receiver,
            isTest(receiver.getUser()) ? "TEST_EXPERIENCE_TIP_INCOME" : "EXPERIENCE_TIP_INCOME_PENDING",
            isTest(receiver.getUser()) ? "IN" : "HOLD",
            receiverIncome,
            "EXPERIENCE_TIP",
            tip.getId(),
            isTest(receiver.getUser())
                ? "收到测试经历打赏（已扣平台服务费）"
                : "经历打赏收入（已扣平台服务费）待解冻"
        );
        if (!isTest(receiver.getUser())) {
            WalletIncomeHold hold = new WalletIncomeHold();
            hold.setUser(receiver.getUser());
            hold.setInquiry(null);
            hold.setReferenceType("EXPERIENCE_TIP");
            hold.setReferenceId(tip.getId());
            hold.setAmount(receiverIncome);
            hold.setReleaseAt(LocalDateTime.now().plusHours(settings == null ? 24 : settings.incomeHoldHours()));
            incomeHolds.save(hold);
        }
        ledger.record(
            "EXPERIENCE_TIP",
            tip.getId(),
            "TRANSFER",
            "经历打赏",
            List.of(
                entry("USER_RECHARGE_LIABILITY", payerUserId, rechargeAmount),
                entry("USER_INCOME_LIABILITY", payerUserId, incomeAmount),
                entry(isTest(receiver.getUser()) ? "USER_INCOME_LIABILITY" : "ANSWERER_PENDING",
                    receiverUserId, negative(receiverIncome)),
                entry("PLATFORM_SERVICE_FEE", null, negative(feeAmount))
            )
        );
        return ExperienceTipView.of(tip);
    }

    private void verifySameTip(ExperienceTip existing, Long certificationId, BigDecimal amount) {
        if (!existing.getCertification().getId().equals(certificationId)
            || !MoneyAmounts.same(existing.getAmount(), amount)) {
            throw BusinessException.badRequest("重复打赏请求的内容不一致");
        }
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
    public FrozenAllocation freezeAudioAppointment(Long userId, BigDecimal amount, Long appointmentId) {
        WalletAccount wallet = lock(userId);
        ensureSources(wallet);
        amount = MoneyAmounts.requirePositive(amount);
        if (alreadyRecorded(
            wallet,
            "AUDIO_APPOINTMENT_FREEZE",
            "AUDIO_APPOINTMENT",
            appointmentId,
            amount
        )) {
            throw BusinessException.badRequest("该语音通话金额已经冻结");
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
        record(
            wallet,
            "AUDIO_APPOINTMENT_FREEZE",
            "FREEZE",
            amount,
            "AUDIO_APPOINTMENT",
            appointmentId,
            "语音通话金额冻结"
        );
        ledger.record(
            "AUDIO_APPOINTMENT",
            appointmentId,
            "FREEZE",
            "语音通话金额冻结",
            List.of(
                entry("USER_RECHARGE_LIABILITY", userId, rechargeAmount),
                entry("USER_INCOME_LIABILITY", userId, incomeAmount),
                entry("INQUIRY_FROZEN", null, negative(amount))
            )
        );
        return new FrozenAllocation(rechargeAmount, incomeAmount);
    }

    @Transactional
    public FrozenAllocation reserveMeteredAudio(Long userId, Long appointmentId) {
        WalletAccount account = lock(userId);
        ensureSources(account);
        BigDecimal amount = MoneyAmounts.normalize(account.getAvailableBalance());
        if (amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw BusinessException.badRequest("余额不足，请先充值");
        }
        return freezeAudioAppointment(userId, amount, appointmentId);
    }

    @Transactional
    public FrozenAllocation freezeInquiryDeposit(Long userId, BigDecimal amount, Long inquiryId) {
        WalletAccount wallet = lock(userId);
        ensureSources(wallet);
        amount = MoneyAmounts.requirePositive(amount);
        if (alreadyRecorded(wallet, "INQUIRY_DEPOSIT_FREEZE", "INQUIRY", inquiryId, amount)) {
            throw BusinessException.badRequest("该询问的押金已经冻结");
        }
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw BusinessException.badRequest("余额不足2元，请先充值后再发起询问");
        }
        BigDecimal rechargeAmount = min(wallet.getRechargeBalance(), amount);
        BigDecimal incomeAmount = MoneyAmounts.subtract(amount, rechargeAmount);
        wallet.setRechargeBalance(MoneyAmounts.subtract(wallet.getRechargeBalance(), rechargeAmount));
        wallet.setIncomeBalance(MoneyAmounts.subtract(wallet.getIncomeBalance(), incomeAmount));
        wallet.setFrozenRechargeBalance(MoneyAmounts.add(wallet.getFrozenRechargeBalance(), rechargeAmount));
        wallet.setFrozenIncomeBalance(MoneyAmounts.add(wallet.getFrozenIncomeBalance(), incomeAmount));
        syncTotals(wallet);
        record(
            wallet,
            "INQUIRY_DEPOSIT_FREEZE",
            "FREEZE",
            amount,
            "INQUIRY",
            inquiryId,
            "询问押金暂存"
        );
        ledger.record(
            "INQUIRY",
            inquiryId,
            "DEPOSIT_FREEZE",
            "询问押金暂存",
            List.of(
                entry("USER_RECHARGE_LIABILITY", userId, rechargeAmount),
                entry("USER_INCOME_LIABILITY", userId, incomeAmount),
                entry("INQUIRY_FROZEN", null, negative(amount))
            )
        );
        return new FrozenAllocation(rechargeAmount, incomeAmount);
    }

    @Transactional
    public void refundInquiryDeposit(
        Long userId,
        BigDecimal rechargeAmount,
        BigDecimal incomeAmount,
        Long inquiryId
    ) {
        WalletAccount wallet = lock(userId);
        ensureSources(wallet);
        BigDecimal total = MoneyAmounts.add(rechargeAmount, incomeAmount);
        if (total.compareTo(BigDecimal.ZERO) <= 0) return;
        if (alreadyRecorded(wallet, "INQUIRY_DEPOSIT_REFUND", "INQUIRY", inquiryId, total)) return;
        ensureSourceFrozen(wallet, rechargeAmount, incomeAmount);
        wallet.setFrozenRechargeBalance(MoneyAmounts.subtract(wallet.getFrozenRechargeBalance(), rechargeAmount));
        wallet.setFrozenIncomeBalance(MoneyAmounts.subtract(wallet.getFrozenIncomeBalance(), incomeAmount));
        wallet.setRechargeBalance(MoneyAmounts.add(wallet.getRechargeBalance(), rechargeAmount));
        wallet.setIncomeBalance(MoneyAmounts.add(wallet.getIncomeBalance(), incomeAmount));
        syncTotals(wallet);
        record(
            wallet,
            "INQUIRY_DEPOSIT_REFUND",
            "IN",
            total,
            "INQUIRY",
            inquiryId,
            "询问押金退回"
        );
        ledger.record(
            "INQUIRY",
            inquiryId,
            "DEPOSIT_REFUND",
            "询问押金退回",
            List.of(
                entry("INQUIRY_FROZEN", null, total),
                entry("USER_RECHARGE_LIABILITY", userId, negative(rechargeAmount)),
                entry("USER_INCOME_LIABILITY", userId, negative(incomeAmount))
            )
        );
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
    public void refundAudioAppointment(
        Long userId,
        BigDecimal rechargeAmount,
        BigDecimal incomeAmount,
        Long appointmentId
    ) {
        WalletAccount wallet = lock(userId);
        ensureSources(wallet);
        BigDecimal total = MoneyAmounts.add(rechargeAmount, incomeAmount);
        if (total.compareTo(BigDecimal.ZERO) <= 0) return;
        if (alreadyRecorded(
            wallet,
            "AUDIO_APPOINTMENT_REFUND",
            "AUDIO_APPOINTMENT",
            appointmentId,
            total
        )) return;
        ensureSourceFrozen(wallet, rechargeAmount, incomeAmount);
        wallet.setFrozenRechargeBalance(MoneyAmounts.subtract(wallet.getFrozenRechargeBalance(), rechargeAmount));
        wallet.setFrozenIncomeBalance(MoneyAmounts.subtract(wallet.getFrozenIncomeBalance(), incomeAmount));
        wallet.setRechargeBalance(MoneyAmounts.add(wallet.getRechargeBalance(), rechargeAmount));
        wallet.setIncomeBalance(MoneyAmounts.add(wallet.getIncomeBalance(), incomeAmount));
        syncTotals(wallet);
        record(
            wallet,
            "AUDIO_APPOINTMENT_REFUND",
            "IN",
            total,
            "AUDIO_APPOINTMENT",
            appointmentId,
            "语音通话金额退回"
        );
        ledger.record(
            "AUDIO_APPOINTMENT",
            appointmentId,
            "REFUND",
            "语音通话金额退回",
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
        settleWithReference(
            questionerId,
            answererId,
            rechargeAmount,
            incomeAmount,
            inquiry,
            "INQUIRY",
            inquiry.getId()
        );
    }

    @Transactional
    public void settleAudioAppointment(
        Long questionerId,
        Long answererId,
        BigDecimal rechargeAmount,
        BigDecimal incomeAmount,
        Inquiry inquiry,
        Long appointmentId
    ) {
        settleWithReference(
            questionerId,
            answererId,
            rechargeAmount,
            incomeAmount,
            inquiry,
            "AUDIO_APPOINTMENT",
            appointmentId
        );
    }

    @Transactional
    public void settleMeteredAudio(
        Long questionerId,
        Long answererId,
        BigDecimal reservedRecharge,
        BigDecimal reservedIncome,
        BigDecimal actualAmount,
        Inquiry inquiry,
        Long appointmentId
    ) {
        actualAmount = MoneyAmounts.normalize(actualAmount);
        BigDecimal reservedTotal = MoneyAmounts.add(reservedRecharge, reservedIncome);
        if (actualAmount.compareTo(BigDecimal.ZERO) < 0 || actualAmount.compareTo(reservedTotal) > 0) {
            throw BusinessException.badRequest("语音通话结算金额异常");
        }
        BigDecimal chargedRecharge = min(reservedRecharge, actualAmount);
        BigDecimal chargedIncome = MoneyAmounts.subtract(actualAmount, chargedRecharge);
        BigDecimal refundRecharge = MoneyAmounts.subtract(reservedRecharge, chargedRecharge);
        BigDecimal refundIncome = MoneyAmounts.subtract(reservedIncome, chargedIncome);
        if (MoneyAmounts.add(refundRecharge, refundIncome).compareTo(BigDecimal.ZERO) > 0) {
            refundAudioAppointment(questionerId, refundRecharge, refundIncome, appointmentId);
        }
        if (actualAmount.compareTo(BigDecimal.ZERO) == 0) return;
        settleWithReference(
            questionerId, answererId, chargedRecharge, chargedIncome,
            inquiry, "AUDIO_APPOINTMENT", appointmentId
        );
    }

    private void settleWithReference(
        Long questionerId,
        Long answererId,
        BigDecimal rechargeAmount,
        BigDecimal incomeAmount,
        Inquiry inquiry,
        String referenceType,
        Long referenceId
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
        recordTransferRisk(payer.getUser(), receiver.getUser(), referenceType, referenceId);
        boolean audioCall = "AUDIO_APPOINTMENT".equals(referenceType);
        String payerTransactionType = audioCall ? "AUDIO_CALL_PAYMENT" : "INQUIRY_PAYMENT";
        String receiverTransactionType = isTest(receiver.getUser())
            ? (audioCall ? "TEST_AUDIO_CALL_INCOME" : "TEST_INQUIRY_INCOME")
            : (audioCall ? "AUDIO_CALL_INCOME_PENDING" : "INQUIRY_INCOME_PENDING");
        boolean payerRecorded = alreadyRecorded(
            payer, payerTransactionType, referenceType, referenceId, amount
        );
        boolean receiverRecorded = alreadyRecorded(
            receiver, receiverTransactionType, referenceType, referenceId, answererIncome
        );
        if (payerRecorded && receiverRecorded) {
            savePlatformFeeRecord(
                inquiry,
                referenceType,
                referenceId,
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
        record(
            payer,
            payerTransactionType,
            "OUT",
            amount,
            referenceType,
            referenceId,
            audioCall ? "语音通话支出" : "询问支出"
        );

        if (isTest(receiver.getUser())) {
            receiver.setIncomeBalance(MoneyAmounts.add(receiver.getIncomeBalance(), answererIncome));
            syncTotals(receiver);
            record(
                receiver, receiverTransactionType, "IN", answererIncome,
                referenceType, referenceId, audioCall ? "测试语音通话净收入" : "测试询问净收入"
            );
            savePlatformFeeRecord(
                inquiry,
                referenceType,
                referenceId,
                amount,
                serviceFee,
                answererIncome,
                "EARNED"
            );
            ledger.record(
                referenceType,
                referenceId,
                "SETTLE",
                audioCall ? "测试语音通话结算" : "测试询问结算",
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
            receiver, receiverTransactionType, "HOLD", answererIncome,
            referenceType, referenceId, audioCall ? "语音通话净收入待解冻" : "回答净收入待解冻"
        );
        WalletIncomeHold hold = new WalletIncomeHold();
        hold.setUser(receiver.getUser());
        hold.setInquiry(inquiry);
        hold.setReferenceType(referenceType);
        hold.setReferenceId(referenceId);
        hold.setAmount(answererIncome);
        AppGlobalSettingService.Settings settings = settings();
        hold.setReleaseAt(LocalDateTime.now().plusHours(settings == null ? 24 : settings.incomeHoldHours()));
        incomeHolds.save(hold);
        savePlatformFeeRecord(
            inquiry,
            referenceType,
            referenceId,
            amount,
            serviceFee,
            answererIncome,
            "PENDING"
        );
        ledger.record(
            referenceType,
            referenceId,
            "SETTLE",
            audioCall ? "语音通话结算待解冻" : "询问结算待解冻",
            List.of(
                entry("INQUIRY_FROZEN", null, amount),
                entry("ANSWERER_PENDING", answererId, negative(answererIncome)),
                entry("PLATFORM_FEE_PENDING", null, negative(serviceFee))
            )
        );
    }

    private void savePlatformFeeRecord(
        Inquiry inquiry,
        String referenceType,
        Long referenceId,
        BigDecimal grossAmount,
        BigDecimal serviceFeeAmount,
        BigDecimal answererIncomeAmount,
        String status
    ) {
        if (platformFeeRecords.existsByReferenceTypeAndReferenceId(referenceType, referenceId)) return;
        PlatformFeeRecord record = new PlatformFeeRecord();
        record.setInquiry(inquiry);
        record.setReferenceType(referenceType);
        record.setReferenceId(referenceId);
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
    public void creditFirstExperienceReward(Long userId, BigDecimal amount, Long referenceId) {
        creditFirstExperienceReward(userId, amount, serviceFeePolicy.currentRate("ANDROID"), referenceId);
    }

    @Transactional
    public void creditFirstExperienceReward(Long userId, BigDecimal amount, BigDecimal feeRate, Long referenceId) {
        User user = user(userId);
        WalletAccount wallet = lock(userId);
        ensureSources(wallet);
        amount = MoneyAmounts.requirePositive(amount);
        String transactionType = isTest(user)
            ? "TEST_FIRST_EXPERIENCE_REWARD"
            : "FIRST_EXPERIENCE_REWARD";
        BigDecimal feeAmount = MoneyAmounts.normalize(amount.multiply(feeRate));
        BigDecimal incomeAmount = MoneyAmounts.subtract(amount, feeAmount);
        if (alreadyRecorded(
            wallet,
            transactionType,
            "FIRST_EXPERIENCE_REWARD",
            referenceId,
            incomeAmount
        )) {
            return;
        }

        wallet.setIncomeBalance(MoneyAmounts.add(wallet.getIncomeBalance(), incomeAmount));
        syncTotals(wallet);
        record(
            wallet,
            transactionType,
            "IN",
            incomeAmount,
            "FIRST_EXPERIENCE_REWARD",
            referenceId,
            isTest(user)
                ? "测试首次发布经历奖励（已扣平台服务费）"
                : "首次发布经历奖励（已扣平台服务费）"
        );
        ledger.record(
            "FIRST_EXPERIENCE_REWARD",
            referenceId,
            "PAID",
            isTest(user) ? "测试首次发布经历奖励到账" : "首次发布经历奖励到账",
            List.of(
                entry(isTest(user) ? "TEST_PROMOTION_EXPENSE" : "PLATFORM_PROMOTION_EXPENSE",
                    null, amount),
                entry("USER_INCOME_LIABILITY", userId, negative(incomeAmount)),
                entry("PLATFORM_SERVICE_FEE", null, negative(feeAmount))
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
        boolean audio = "AUDIO_APPOINTMENT".equals(hold.getReferenceType());
        boolean tip = "EXPERIENCE_TIP".equals(hold.getReferenceType());
        record(
            wallet,
            audio ? "AUDIO_CALL_INCOME_RELEASE" : tip
                ? "EXPERIENCE_TIP_INCOME_RELEASE" : "INQUIRY_INCOME_RELEASE",
            "IN",
            hold.getAmount(),
            hold.getReferenceType(),
            hold.getReferenceId(),
            audio ? "语音通话收入已解冻" : tip ? "经历打赏收入已解冻" : "回答收入已解冻"
        );
        PlatformFeeRecord fee = platformFeeRecords.findByReferenceTypeAndReferenceId(
            hold.getReferenceType(), hold.getReferenceId()
        ).orElse(null);
        if (fee != null && "PENDING".equals(fee.getStatus())) {
            fee.setStatus("EARNED");
            fee.setFinalizedAt(LocalDateTime.now());
        }
        BigDecimal serviceFee = fee == null
            ? hold.getInquiry() == null ? MoneyAmounts.ZERO : hold.getInquiry().getServiceFeeAmount()
            : fee.getServiceFeeAmount();
        ledger.record(
            hold.getReferenceType(),
            hold.getReferenceId(),
            "RELEASE",
            audio ? "语音通话收入解冻" : tip ? "经历打赏收入解冻" : "回答收入解冻",
            List.of(
                entry("ANSWERER_PENDING", hold.getUser().getId(), hold.getAmount()),
                entry("PLATFORM_FEE_PENDING", null, serviceFee),
                entry("USER_INCOME_LIABILITY", hold.getUser().getId(), negative(hold.getAmount())),
                entry("PLATFORM_SERVICE_FEE", null, negative(serviceFee))
            )
        );
    }

    private void recordTransferRisk(User payer, User receiver, String referenceType, Long referenceId) {
        List<String> reasons = new java.util.ArrayList<>();
        if (sameMeaningful(payer.getRegisterDeviceId(), receiver.getRegisterDeviceId())
            || sameMeaningful(payer.getLastLoginDeviceId(), receiver.getLastLoginDeviceId())
            || sameMeaningful(payer.getRegisterDeviceId(), receiver.getLastLoginDeviceId())
            || sameMeaningful(payer.getLastLoginDeviceId(), receiver.getRegisterDeviceId())) {
            reasons.add("付款方与收款方设备关联");
        }
        if (sameMeaningfulIp(payer.getRegisterIp(), receiver.getRegisterIp())
            || sameMeaningfulIp(payer.getLastLoginIp(), receiver.getLastLoginIp())) {
            reasons.add("付款方与收款方IP关联");
        }
        if (!reasons.isEmpty()) {
            securityEvents.recordSafely(
                receiver.getId(), null, "SUSPICIOUS_FUNDS_TRANSFER", "CRITICAL",
                receiver.getLastLoginIp(), receiver.getLastLoginDeviceId(),
                "referenceType=" + referenceType + ",referenceId=" + referenceId +
                    ",payerId=" + payer.getId() + ",reasons=" + String.join("|", reasons)
            );
        }
    }

    private boolean sameMeaningful(String left, String right) {
        return left != null && right != null && !left.isBlank() && !right.isBlank()
            && !"unknown".equalsIgnoreCase(left) && left.equals(right);
    }

    private boolean sameMeaningfulIp(String left, String right) {
        return sameMeaningful(left, right) && !left.startsWith("127.") && !"::1".equals(left);
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

    private BigDecimal withdrawalAmount(BigDecimal amount, AppGlobalSettingService.Settings settings) {
        return MoneyAmounts.requireWholeAmount(
            amount,
            settings == null ? BigDecimal.ONE : settings.withdrawalMinAmount(),
            settings == null ? new BigDecimal("9999") : settings.withdrawalMaxAmount(),
            "提现金额"
        );
    }

    private AppGlobalSettingService.Settings settings() {
        return globalSettings == null ? null : globalSettings.current();
    }

    private String requireRequestId(String requestId) {
        return requireRequestId(requestId, "提现");
    }

    private String requireRequestId(String requestId, String businessName) {
        String value = requestId == null ? "" : requestId.trim();
        if (!value.matches("[A-Za-z0-9_-]{12,64}")) {
            throw BusinessException.badRequest(businessName + "请求标识无效");
        }
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

    public record ExperienceTipView(
        Long id,
        Long certificationId,
        BigDecimal amount,
        BigDecimal feeRate,
        BigDecimal feeAmount,
        BigDecimal receiverIncomeAmount,
        LocalDateTime createdAt
    ) {
        static ExperienceTipView of(ExperienceTip item) {
            return new ExperienceTipView(
                item.getId(),
                item.getCertification().getId(),
                item.getAmount(),
                item.getFeeRate(),
                item.getFeeAmount(),
                item.getReceiverIncomeAmount(),
                item.getCreatedAt()
            );
        }
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
        String riskLevel,
        String riskReasons,
        String status,
        String batchNo,
        LocalDateTime exportedAt,
        LocalDateTime createdAt
    ) {
        static WithdrawalView of(Withdrawal item) {
            return new WithdrawalView(
                item.getId(), item.getAmount(),
                item.getPayeeNameSnapshot(), item.getAlipayAccountMaskedSnapshot(), item.getRiskLevel(), item.getRiskReasons(), item.getStatus(),
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
