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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RechargeService {
    private final RechargeRepository recharges;
    private final UserRepository users;
    private final WalletService wallet;
    private final PaymentGateway gateway;

    public PaymentGateway.PaymentCapability capability() {
        return gateway.capability();
    }

    @Transactional
    public RechargeView create(Long userId, BigDecimal rawAmount) {
        if (!gateway.capability().available()) {
            throw BusinessException.serviceUnavailable(gateway.capability().message());
        }
        if (rawAmount == null) throw BusinessException.badRequest("请输入充值金额");
        try {
            rawAmount.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw BusinessException.badRequest("充值金额只支持整数");
        }
        if (rawAmount.compareTo(BigDecimal.ONE) < 0 || rawAmount.compareTo(new BigDecimal("9999")) > 0) {
            throw BusinessException.badRequest("充值金额必须在1至9999之间");
        }
        BigDecimal amount = rawAmount.setScale(2, RoundingMode.UNNECESSARY);
        User user = users.findById(userId).orElseThrow(() -> BusinessException.notFound("用户不存在"));
        Recharge item = new Recharge(); item.setUser(user); item.setAmount(amount);
        item.setOrderNo("SXW" + UUID.randomUUID().toString().replace("-", "").toUpperCase()); item.setStatus("PENDING");
        item = recharges.save(item);
        PaymentGateway.PaymentOrder order = gateway.createOrder(item.getOrderNo(), amount, "事先问账户充值");
        return RechargeView.of(item, order.paymentPayload());
    }

    public List<RechargeView> list(Long userId) { return recharges.findByUserIdOrderByCreatedAtDesc(userId).stream().map(i -> RechargeView.of(i, null)).toList(); }

    @Transactional
    public RechargeView find(Long userId, String orderNo) {
        Recharge item = recharges.findByOrderNo(orderNo)
            .filter(recharge -> recharge.getUser().getId().equals(userId))
            .orElseThrow(() -> BusinessException.notFound("充值订单不存在"));
        if ("PENDING".equals(item.getStatus()) && !(gateway instanceof MockAlipayGateway)) {
            PaymentGateway.PaymentStatus status = gateway.queryOrder(orderNo);
            if ("PAID".equals(status.status())) {
                applyPaid(item, status.providerTradeNo(), status.paidAmount(), status.paidAt());
            }
        }
        return RechargeView.of(item, null);
    }

    public RechargeView mockOrder(String orderNo) {
        ensureMockGateway();
        Recharge item = recharges.findByOrderNo(orderNo)
            .orElseThrow(() -> BusinessException.notFound("充值订单不存在"));
        return RechargeView.of(item, null);
    }

    @Transactional
    public void completeMockPayment(String orderNo) {
        ensureMockGateway();
        Recharge item = recharges.findByOrderNo(orderNo)
            .orElseThrow(() -> BusinessException.notFound("充值订单不存在"));
        applyPaid(item, null, item.getAmount(), LocalDateTime.now());
    }

    @Transactional
    public void paidCallback(String payload, java.util.Map<String, String> headers) {
        PaymentGateway.PaymentNotification notification = gateway.verifyNotification(payload, headers);
        if (!"PAID".equals(notification.status())) return;
        Recharge item = recharges.findByOrderNo(notification.orderNo())
            .orElseThrow(() -> BusinessException.notFound("充值订单不存在"));
        applyPaid(
            item,
            notification.providerTradeNo(),
            notification.paidAmount(),
            notification.paidAt()
        );
    }

    private void applyPaid(
        Recharge item,
        String providerTradeNo,
        BigDecimal paidAmount,
        LocalDateTime paidAt
    ) {
        if ("PAID".equals(item.getStatus())) return;
        if (!"PENDING".equals(item.getStatus())) throw BusinessException.badRequest("订单状态异常");
        if (paidAmount == null || item.getAmount().compareTo(paidAmount) != 0) {
            throw BusinessException.badRequest("支付金额与充值订单不一致");
        }
        item.setStatus("PAID");
        item.setProviderTradeNo(providerTradeNo);
        item.setPaidAt(paidAt == null ? LocalDateTime.now() : paidAt);
        wallet.creditRecharge(item.getUser().getId(), item.getAmount(), item.getId());
    }

    private void ensureMockGateway() {
        if (!(gateway instanceof MockAlipayGateway)) {
            throw BusinessException.notFound("页面不存在");
        }
    }

    public record RechargeView(Long id, String orderNo, String providerTradeNo, String channel,
                               BigDecimal amount, String status, String paymentPayload,
                               LocalDateTime paidAt, LocalDateTime createdAt) {
        static RechargeView of(Recharge i, String payload) {
            return new RechargeView(
                i.getId(), i.getOrderNo(), i.getProviderTradeNo(), i.getChannel(), i.getAmount(),
                i.getStatus(), payload, i.getPaidAt(), i.getCreatedAt()
            );
        }
    }
}
