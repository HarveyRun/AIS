package com.shixianwen.wallet;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletAccountRepository wallets;
    private final WalletTransactionRepository transactions;
    private final BankCardRepository bankCards;
    private final WithdrawalRepository withdrawals;
    private final UserRepository users;
    private final WithdrawalFeePolicy withdrawalFeePolicy;

    public WalletView get(Long userId) {
        WalletAccount wallet = wallets.findByUserId(userId).orElseThrow(() -> BusinessException.notFound("账户不存在"));
        return new WalletView(
            wallet.getAvailableBalance(),
            wallet.getFrozenBalance(),
            wallet.getTotalWithdrawn(),
            withdrawalFeePolicy.freeLimit(),
            withdrawalFeePolicy.rate()
        );
    }

    public List<TransactionView> transactions(Long userId) {
        return transactions.findByUserIdOrderByCreatedAtDesc(userId).stream().map(TransactionView::of).toList();
    }

    public BankCardView bankCard(Long userId) {
        return bankCards.findByUserId(userId).map(BankCardView::of).orElse(null);
    }

    @Transactional
    public BankCardView bindBankCard(Long userId, String holderName, String bankName, String cardNumber) {
        String number = cardNumber == null ? "" : cardNumber.replaceAll("\\s", "");
        if (!number.matches("\\d{12,19}")) throw BusinessException.badRequest("请输入正确的银行卡号");
        User user = users.findById(userId).orElseThrow(() -> BusinessException.notFound("用户不存在"));
        BankCard card = bankCards.findByUserId(userId).orElseGet(BankCard::new);
        card.setUser(user);
        card.setHolderName(require(holderName, "持卡人姓名"));
        card.setBankName(require(bankName, "开户银行"));
        card.setCardNumberCiphertext("masked:" + number.substring(number.length() - 4));
        card.setCardLastFour(number.substring(number.length() - 4));
        return BankCardView.of(bankCards.save(card));
    }

    @Transactional
    public WithdrawalView withdraw(Long userId, BigDecimal amount) {
        return withdraw(userId, amount, UUID.randomUUID().toString());
    }

    @Transactional
    public WithdrawalView withdraw(Long userId, BigDecimal amount, String requestId) {
        amount = withdrawalAmount(amount);
        String normalizedRequestId = requireRequestId(requestId);
        BankCard card = bankCards.findByUserId(userId).orElseThrow(() -> BusinessException.badRequest("请先绑定银行卡"));
        WalletAccount wallet = lock(userId);
        Withdrawal existing = withdrawals.findByUserIdAndRequestNo(userId, normalizedRequestId).orElse(null);
        if (existing != null) {
            if (!MoneyAmounts.same(existing.getAmount(), amount)) {
                throw BusinessException.badRequest("重复提现请求的金额不一致");
            }
            return WithdrawalView.of(existing);
        }
        if (wallet.getAvailableBalance().compareTo(amount) < 0) throw BusinessException.badRequest("余额不足");
        BigDecimal fee = withdrawalFeePolicy.calculate(wallet.getTotalWithdrawn(), amount);
        wallet.setAvailableBalance(MoneyAmounts.subtract(wallet.getAvailableBalance(), amount));
        wallet.setTotalWithdrawn(MoneyAmounts.add(wallet.getTotalWithdrawn(), amount));
        Withdrawal item = new Withdrawal();
        item.setUser(wallet.getUser()); item.setBankCard(card); item.setRequestNo(normalizedRequestId);
        item.setAmount(amount); item.setFee(fee);
        item.setArrivalAmount(MoneyAmounts.subtract(amount, fee)); item.setBankNameSnapshot(card.getBankName());
        item.setCardLastFourSnapshot(card.getCardLastFour()); item.setStatus("PROCESSING");
        withdrawals.save(item);
        record(wallet, "WITHDRAWAL", "OUT", amount, "WITHDRAWAL", item.getId(), "提现到银行卡");
        return WithdrawalView.of(item);
    }

    public List<WithdrawalView> withdrawals(Long userId) {
        return withdrawals.findByUserIdOrderByCreatedAtDesc(userId).stream().map(WithdrawalView::of).toList();
    }

    @Transactional(readOnly = true)
    public WithdrawalQuoteView quoteWithdrawal(Long userId, BigDecimal amount) {
        BigDecimal normalizedAmount = withdrawalAmount(amount);
        WalletAccount wallet = wallets.findByUserId(userId)
            .orElseThrow(() -> BusinessException.notFound("账户不存在"));
        BigDecimal fee = withdrawalFeePolicy.calculate(wallet.getTotalWithdrawn(), normalizedAmount);
        return new WithdrawalQuoteView(
            normalizedAmount,
            fee,
            MoneyAmounts.subtract(normalizedAmount, fee)
        );
    }

    @Transactional
    public void freeze(Long userId, BigDecimal amount, Long referenceId) {
        WalletAccount wallet = lock(userId);
        amount = MoneyAmounts.requirePositive(amount);
        if (alreadyRecorded(wallet, "INQUIRY_FREEZE", "INQUIRY", referenceId, amount)) return;
        if (wallet.getAvailableBalance().compareTo(amount) < 0) throw BusinessException.badRequest("余额不足，请先充值");
        wallet.setAvailableBalance(MoneyAmounts.subtract(wallet.getAvailableBalance(), amount));
        wallet.setFrozenBalance(MoneyAmounts.add(wallet.getFrozenBalance(), amount));
        record(wallet, "INQUIRY_FREEZE", "FREEZE", amount, "INQUIRY", referenceId, "询问金额冻结");
    }

    @Transactional
    public void refund(Long userId, BigDecimal amount, Long referenceId) {
        WalletAccount wallet = lock(userId); amount = MoneyAmounts.requirePositive(amount);
        if (alreadyRecorded(wallet, "INQUIRY_REFUND", "INQUIRY", referenceId, amount)) return;
        ensureFrozen(wallet, amount);
        wallet.setFrozenBalance(MoneyAmounts.subtract(wallet.getFrozenBalance(), amount));
        wallet.setAvailableBalance(MoneyAmounts.add(wallet.getAvailableBalance(), amount));
        record(wallet, "INQUIRY_REFUND", "IN", amount, "INQUIRY", referenceId, "询问金额退回");
    }

    @Transactional
    public void settle(Long questionerId, Long answererId, BigDecimal amount, Long referenceId) {
        amount = MoneyAmounts.requirePositive(amount);
        WalletPair pair = lockPair(questionerId, answererId);
        WalletAccount payer = pair.forUser(questionerId);
        WalletAccount receiver = pair.forUser(answererId);
        boolean payerRecorded = alreadyRecorded(
            payer, "INQUIRY_PAYMENT", "INQUIRY", referenceId, amount
        );
        boolean receiverRecorded = alreadyRecorded(
            receiver, "INQUIRY_INCOME", "INQUIRY", referenceId, amount
        );
        if (payerRecorded && receiverRecorded) return;
        if (payerRecorded || receiverRecorded) {
            throw BusinessException.badRequest("该询问的资金流水不完整，请联系平台处理");
        }
        ensureFrozen(payer, amount);
        payer.setFrozenBalance(MoneyAmounts.subtract(payer.getFrozenBalance(), amount));
        record(payer, "INQUIRY_PAYMENT", "OUT", amount, "INQUIRY", referenceId, "询问支出");
        receiver.setAvailableBalance(MoneyAmounts.add(receiver.getAvailableBalance(), amount));
        record(receiver, "INQUIRY_INCOME", "IN", amount, "INQUIRY", referenceId, "回答收入");
    }

    @Transactional
    public void creditRecharge(Long userId, BigDecimal amount, Long referenceId) {
        WalletAccount wallet = lock(userId); amount = MoneyAmounts.requirePositive(amount);
        if (alreadyRecorded(wallet, "RECHARGE", "RECHARGE", referenceId, amount)) return;
        wallet.setAvailableBalance(MoneyAmounts.add(wallet.getAvailableBalance(), amount));
        record(wallet, "RECHARGE", "IN", amount, "RECHARGE", referenceId, "支付宝充值");
    }

    private WalletAccount lock(Long userId) {
        return wallets.findWithLockByUserId(userId).orElseThrow(() -> BusinessException.notFound("账户不存在"));
    }
    private void ensureFrozen(WalletAccount wallet, BigDecimal amount) {
        if (wallet.getFrozenBalance().compareTo(amount) < 0) throw BusinessException.badRequest("冻结金额异常");
    }
    private WalletPair lockPair(Long firstUserId, Long secondUserId) {
        if (firstUserId.equals(secondUserId)) {
            throw BusinessException.badRequest("付款人与收款人不能相同");
        }
        Long lowerId = Math.min(firstUserId, secondUserId);
        Long higherId = Math.max(firstUserId, secondUserId);
        WalletAccount lower = lock(lowerId);
        WalletAccount higher = lock(higherId);
        return new WalletPair(lower, higher);
    }
    private BigDecimal withdrawalAmount(BigDecimal amount) {
        return MoneyAmounts.requireWholeAmount(
            amount,
            BigDecimal.ONE,
            new BigDecimal("9999"),
            "提现金额"
        );
    }
    private String requireRequestId(String requestId) {
        String value = requestId == null ? "" : requestId.trim();
        if (!value.matches("[A-Za-z0-9_-]{12,64}")) {
            throw BusinessException.badRequest("提现请求标识无效");
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
        return transactions
            .findByUserIdAndTransactionTypeAndReferenceTypeAndReferenceId(
                wallet.getUser().getId(), type, referenceType, referenceId
            )
            .map(existing -> {
                if (!MoneyAmounts.same(existing.getAmount(), amount)) {
                    throw BusinessException.badRequest("重复资金操作的金额不一致");
                }
                return true;
            })
            .orElse(false);
    }
    private String require(String value, String label) {
        if (value == null || value.isBlank()) throw BusinessException.badRequest("请填写" + label);
        return value.trim();
    }
    private void record(WalletAccount wallet, String type, String direction, BigDecimal amount,
                        String referenceType, Long referenceId, String description) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUser(wallet.getUser()); tx.setTransactionType(type); tx.setDirection(direction); tx.setAmount(amount);
        tx.setAvailableAfter(wallet.getAvailableBalance()); tx.setFrozenAfter(wallet.getFrozenBalance());
        tx.setReferenceType(referenceType); tx.setReferenceId(referenceId); tx.setDescription(description);
        transactions.save(tx);
    }

    public record WalletView(
        BigDecimal availableBalance,
        BigDecimal frozenBalance,
        BigDecimal totalWithdrawn,
        BigDecimal freeWithdrawalLimit,
        BigDecimal withdrawalFeeRate
    ) {}
    public record TransactionView(Long id, String type, String direction, BigDecimal amount, BigDecimal availableAfter,
                                  BigDecimal frozenAfter, String description, LocalDateTime createdAt) {
        static TransactionView of(WalletTransaction t) { return new TransactionView(t.getId(), t.getTransactionType(), t.getDirection(), t.getAmount(), t.getAvailableAfter(), t.getFrozenAfter(), t.getDescription(), t.getCreatedAt()); }
    }
    public record BankCardView(Long id, String holderName, String bankName, String lastFour) {
        static BankCardView of(BankCard c) { return new BankCardView(c.getId(), c.getHolderName(), c.getBankName(), c.getCardLastFour()); }
    }
    public record WithdrawalView(Long id, BigDecimal amount, BigDecimal fee, BigDecimal arrivalAmount, String bankName,
                                 String lastFour, String status, LocalDateTime createdAt) {
        static WithdrawalView of(Withdrawal w) { return new WithdrawalView(w.getId(), w.getAmount(), w.getFee(), w.getArrivalAmount(), w.getBankNameSnapshot(), w.getCardLastFourSnapshot(), w.getStatus(), w.getCreatedAt()); }
    }
    public record WithdrawalQuoteView(BigDecimal amount, BigDecimal fee, BigDecimal arrivalAmount) {}

    private record WalletPair(WalletAccount lower, WalletAccount higher) {
        WalletAccount forUser(Long userId) {
            if (lower.getUser().getId().equals(userId)) return lower;
            if (higher.getUser().getId().equals(userId)) return higher;
            throw BusinessException.notFound("账户不存在");
        }
    }
}
