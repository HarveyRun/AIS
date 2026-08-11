package com.shixianwen.wallet;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RechargeService {
    private final RechargeRepository recharges;
    private final UserRepository users;
    private final WalletService wallet;
    private final PaymentGateway gateway;
    @Value("${app.payment.callback-secret:change-me}") private String callbackSecret;

    @Transactional
    public RechargeView create(Long userId, BigDecimal rawAmount) {
        BigDecimal amount = rawAmount.setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw BusinessException.badRequest("充值金额必须大于0");
        User user = users.findById(userId).orElseThrow(() -> BusinessException.notFound("用户不存在"));
        Recharge item = new Recharge(); item.setUser(user); item.setAmount(amount);
        item.setOrderNo("SXW" + UUID.randomUUID().toString().replace("-", "").toUpperCase()); item.setStatus("PENDING");
        item = recharges.save(item);
        PaymentGateway.PaymentOrder order = gateway.createOrder(item.getOrderNo(), amount, "事先问账户充值");
        return RechargeView.of(item, order.paymentUrl());
    }

    public List<RechargeView> list(Long userId) { return recharges.findByUserIdOrderByCreatedAtDesc(userId).stream().map(i -> RechargeView.of(i, null)).toList(); }

    @Transactional
    public void paidCallback(String secret, String orderNo) {
        if (!callbackSecret.equals(secret)) throw BusinessException.forbidden("回调签名无效");
        Recharge item = recharges.findByOrderNo(orderNo).orElseThrow(() -> BusinessException.notFound("充值订单不存在"));
        if ("PAID".equals(item.getStatus())) return;
        if (!"PENDING".equals(item.getStatus())) throw BusinessException.badRequest("订单状态异常");
        item.setStatus("PAID"); item.setPaidAt(LocalDateTime.now());
        wallet.creditRecharge(item.getUser().getId(), item.getAmount(), item.getId());
    }

    public record RechargeView(Long id, String orderNo, String channel, BigDecimal amount, String status,
                               String paymentUrl, LocalDateTime paidAt, LocalDateTime createdAt) {
        static RechargeView of(Recharge i, String url) { return new RechargeView(i.getId(), i.getOrderNo(), i.getChannel(), i.getAmount(), i.getStatus(), url, i.getPaidAt(), i.getCreatedAt()); }
    }
}
