package com.shixianwen.wallet;

import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RechargeServiceTest {
    @Test
    void mockPaymentRunsTheSameIdempotentCreditFlow() {
        RechargeRepository recharges = mock(RechargeRepository.class);
        UserRepository users = mock(UserRepository.class);
        WalletService wallet = mock(WalletService.class);
        RechargeService service = new RechargeService(
            recharges,
            users,
            wallet,
            new MockAlipayGateway()
        );

        User user = new User();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(recharges.save(any(Recharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RechargeService.RechargeView created = service.create(1L, new BigDecimal("12"));
        assertEquals("PENDING", created.status());
        assertTrue(created.paymentPayload().startsWith("/api/recharges/mock-cashier?orderNo="));

        Recharge stored = new Recharge();
        stored.setUser(user);
        stored.setOrderNo(created.orderNo());
        stored.setAmount(created.amount());
        stored.setStatus("PENDING");
        when(recharges.findByOrderNo(created.orderNo())).thenReturn(Optional.of(stored));

        service.completeMockPayment(created.orderNo());
        service.completeMockPayment(created.orderNo());

        assertEquals("PAID", stored.getStatus());
        verify(wallet, times(1)).creditRecharge(null, new BigDecimal("12.00"), null);
    }

    @Test
    void rechargeRejectsDecimalsAndAmountsOverTheLimit() {
        RechargeService service = new RechargeService(
            mock(RechargeRepository.class),
            mock(UserRepository.class),
            mock(WalletService.class),
            new MockAlipayGateway()
        );

        assertThrows(BusinessException.class, () -> service.create(1L, new BigDecimal("12.34")));
        assertThrows(BusinessException.class, () -> service.create(1L, new BigDecimal("10000")));
    }
}
