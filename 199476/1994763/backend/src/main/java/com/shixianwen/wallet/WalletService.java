package com.shixianwen.wallet;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

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
        return new WalletView(wallet.getAvailableBalance(), wallet.getFrozenBalance(), wallet.getTotalWithdrawn());
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
        amount = money(amount);
        BankCard card = bankCards.findByUserId(userId).orElseThrow(() -> BusinessException.badRequest("请先绑定银行卡"));
        WalletAccount wallet = lock(userId);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) throw BusinessException.badRequest("余额不足");
        BigDecimal fee = withdrawalFeePolicy.calculate(wallet.getTotalWithdrawn(), amount);
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(amount));
        Withdrawal item = new Withdrawal();
        item.setUser(wallet.getUser()); item.setBankCard(card); item.setAmount(amount); item.setFee(fee);
        item.setArrivalAmount(amount.subtract(fee)); item.setBankNameSnapshot(card.getBankName());
        item.setCardLastFourSnapshot(card.getCardLastFour()); item.setStatus("PROCESSING");
        withdrawals.save(item);
        record(wallet, "WITHDRAWAL", "OUT", amount, "WITHDRAWAL", item.getId(), "提现到银行卡");
        return WithdrawalView.of(item);
    }

    public List<WithdrawalView> withdrawals(Long userId) {
        return withdrawals.findByUserIdOrderByCreatedAtDesc(userId).stream().map(WithdrawalView::of).toList();
    }

    @Transactional
    public void freeze(Long userId, BigDecimal amount, Long referenceId) {
        WalletAccount wallet = lock(userId);
        amount = money(amount);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) throw BusinessException.badRequest("余额不足，请先充值");
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        wallet.setFrozenBalance(wallet.getFrozenBalance().add(amount));
        record(wallet, "INQUIRY_FREEZE", "FREEZE", amount, "INQUIRY", referenceId, "询问金额冻结");
    }

    @Transactional
    public void refund(Long userId, BigDecimal amount, Long referenceId) {
        WalletAccount wallet = lock(userId); amount = money(amount);
        ensureFrozen(wallet, amount);
        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        record(wallet, "INQUIRY_REFUND", "IN", amount, "INQUIRY", referenceId, "询问金额退回");
    }

    @Transactional
    public void settle(Long questionerId, Long answererId, BigDecimal amount, Long referenceId) {
        WalletAccount payer = lock(questionerId); amount = money(amount); ensureFrozen(payer, amount);
        payer.setFrozenBalance(payer.getFrozenBalance().subtract(amount));
        record(payer, "INQUIRY_PAYMENT", "OUT", amount, "INQUIRY", referenceId, "询问支出");
        WalletAccount receiver = lock(answererId);
        receiver.setAvailableBalance(receiver.getAvailableBalance().add(amount));
        record(receiver, "INQUIRY_INCOME", "IN", amount, "INQUIRY", referenceId, "回答收入");
    }

    @Transactional
    public void creditRecharge(Long userId, BigDecimal amount, Long referenceId) {
        WalletAccount wallet = lock(userId); amount = money(amount);
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        record(wallet, "RECHARGE", "IN", amount, "RECHARGE", referenceId, "支付宝充值");
    }

    private WalletAccount lock(Long userId) {
        return wallets.findWithLockByUserId(userId).orElseThrow(() -> BusinessException.notFound("账户不存在"));
    }
    private void ensureFrozen(WalletAccount wallet, BigDecimal amount) {
        if (wallet.getFrozenBalance().compareTo(amount) < 0) throw BusinessException.badRequest("冻结金额异常");
    }
    private BigDecimal money(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw BusinessException.badRequest("金额必须大于0");
        return amount.setScale(2, RoundingMode.HALF_UP);
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

    public record WalletView(BigDecimal availableBalance, BigDecimal frozenBalance, BigDecimal totalWithdrawn) {}
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
}
