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
public class RechargeService {
    private final RechargeRepository recharges;
    private final UserRepository users;
    private final WalletService wallet;
    private final PaymentGateway gateway;

    public PaymentGateway.PaymentCapability capability(Long userId) {
        User user = users.findById(userId)
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        if ("TEST".equals(user.getAccountType())) {
            return new PaymentGateway.PaymentCapability(
                "TEST",
                "测试余额",
                true,
                "测试余额即时到账",
                "TEST"
            );
        }
        return gateway.capability();
    }

    @Transactional
    public RechargeView create(Long userId, BigDecimal rawAmount) {
        return create(userId, rawAmount, UUID.randomUUID().toString());
    }

    @Transactional
    public RechargeView create(Long userId, BigDecimal rawAmount, String requestId) {
        BigDecimal amount = MoneyAmounts.requireWholeAmount(
            rawAmount,
            BigDecimal.ONE,
            new BigDecimal("9999"),
            "充值金额"
        );
        String normalizedRequestId = requireRequestId(requestId);
        User user = users.findWithLockById(userId)
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        boolean testAccount = "TEST".equals(user.getAccountType());
        if (!testAccount && !gateway.capability().available()) {
            throw BusinessException.serviceUnavailable(gateway.capability().message());
        }
        Recharge existing = recharges.findByUserIdAndRequestNo(userId, normalizedRequestId).orElse(null);
        if (existing != null) {
            if (!MoneyAmounts.same(existing.getAmount(), amount)) {
                throw BusinessException.badRequest("重复充值请求的金额不一致");
            }
            if ("PAID".equals(existing.getStatus())) return RechargeView.of(existing, null);
            PaymentGateway.PaymentOrder existingOrder = gateway.createOrder(
                existing.getOrderNo(), existing.getAmount(), "事先问账户充值"
            );
            return RechargeView.of(existing, existingOrder.paymentPayload());
        }
        Recharge item = new Recharge(); item.setUser(user); item.setAmount(amount);
        item.setRequestNo(normalizedRequestId);
        item.setOrderNo((testAccount ? "TEST" : "SXW") + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        item.setChannel(testAccount ? "TEST" : "ALIPAY");
        item.setStatus(testAccount ? "PAID" : "PENDING");
        if (testAccount) item.setPaidAt(LocalDateTime.now());
        item = recharges.save(item);
        if (testAccount) {
            wallet.creditRecharge(userId, amount, item.getId());
            return RechargeView.of(item, null);
        }
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
                item = recharges.findWithLockByOrderNo(orderNo)
                    .orElseThrow(() -> BusinessException.notFound("充值订单不存在"));
                applyPaid(item, status.providerTradeNo(), status.paidAmount(), status.paidAt());
            }
        }
        return RechargeView.of(item, null);
    }

    public RechargeView mockOrder(String orderNo) {
        ensureMockGateway();
        Recharge item = recharges.findByOrderNo(orderNo)
            .orElseThrow(() -> BusinessException.notFound("充值订单不存在"));
        rejectTestPaymentCallback(item);
        return RechargeView.of(item, null);
    }

    @Transactional
    public void completeMockPayment(String orderNo) {
        ensureMockGateway();
        Recharge item = recharges.findWithLockByOrderNo(orderNo)
            .orElseThrow(() -> BusinessException.notFound("充值订单不存在"));
        rejectTestPaymentCallback(item);
        applyPaid(item, null, item.getAmount(), LocalDateTime.now());
    }

    @Transactional
    public void paidCallback(String payload, java.util.Map<String, String> headers) {
        PaymentGateway.PaymentNotification notification = gateway.verifyNotification(payload, headers);
        if (!"PAID".equals(notification.status())) return;
        if (notification.orderNo() == null || notification.orderNo().isBlank()) {
            throw BusinessException.badRequest("支付回调缺少平台订单号");
        }
        Recharge item = recharges.findWithLockByOrderNo(notification.orderNo())
            .orElseThrow(() -> BusinessException.notFound("充值订单不存在"));
        rejectTestPaymentCallback(item);
        applyPaid(
            item,
            notification.providerTradeNo(),
            notification.paidAmount(),
            notification.paidAt()
        );
    }

    private void rejectTestPaymentCallback(Recharge item) {
        if ("TEST".equals(item.getUser().getAccountType())) {
            throw BusinessException.forbidden("测试充值不接收外部支付回调");
        }
    }

    private void applyPaid(
        Recharge item,
        String providerTradeNo,
        BigDecimal paidAmount,
        LocalDateTime paidAt
    ) {
        BigDecimal normalizedPaidAmount = MoneyAmounts.requireExactPositive(paidAmount, "实付金额");
        verifyProviderTradeNo(item, providerTradeNo);
        if ("PAID".equals(item.getStatus())) {
            if (!MoneyAmounts.same(item.getAmount(), normalizedPaidAmount)) {
                throw BusinessException.badRequest("重复支付通知的金额不一致");
            }
            return;
        }
        if (!"PENDING".equals(item.getStatus())) throw BusinessException.badRequest("订单状态异常");
        if (!MoneyAmounts.same(item.getAmount(), normalizedPaidAmount)) {
            throw BusinessException.badRequest("支付金额与充值订单不一致");
        }
        item.setStatus("PAID");
        item.setProviderTradeNo(providerTradeNo);
        item.setPaidAt(paidAt == null ? LocalDateTime.now() : paidAt);
        wallet.creditRecharge(item.getUser().getId(), item.getAmount(), item.getId());
    }

    private void verifyProviderTradeNo(Recharge item, String providerTradeNo) {
        boolean mockPayment = gateway instanceof MockAlipayGateway;
        if (!mockPayment && (providerTradeNo == null || providerTradeNo.isBlank())) {
            throw BusinessException.badRequest("支付回调缺少支付宝交易号");
        }
        if (providerTradeNo == null || providerTradeNo.isBlank()) return;
        if (item.getProviderTradeNo() != null && !item.getProviderTradeNo().equals(providerTradeNo)) {
            throw BusinessException.badRequest("重复支付通知的支付宝交易号不一致");
        }
        recharges.findByProviderTradeNo(providerTradeNo)
            .filter(existing -> !existing.getId().equals(item.getId()))
            .ifPresent(existing -> {
                throw BusinessException.badRequest("支付宝交易号已被其他订单使用");
            });
    }

    private void ensureMockGateway() {
        if (!(gateway instanceof MockAlipayGateway)) {
            throw BusinessException.notFound("页面不存在");
        }
    }

    private String requireRequestId(String requestId) {
        String value = requestId == null ? "" : requestId.trim();
        if (!value.matches("[A-Za-z0-9_-]{12,64}")) {
            throw BusinessException.badRequest("充值请求标识无效");
        }
        return value;
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
