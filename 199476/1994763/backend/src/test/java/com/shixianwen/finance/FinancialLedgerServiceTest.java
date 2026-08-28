package com.shixianwen.finance;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class FinancialLedgerServiceTest {
    @Test
    void rejectsUnbalancedVoucherBeforeWritingAnything() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        FinancialLedgerService service = new FinancialLedgerService(jdbc);

        assertThatThrownBy(() -> service.record(
            "INQUIRY",
            8L,
            "SETTLE",
            "询问结算",
            List.of(
                FinancialLedgerService.entry("INQUIRY_FROZEN", null, new BigDecimal("100.00")),
                FinancialLedgerService.entry("USER_INCOME", 2L, new BigDecimal("-95.00"))
            )
        )).hasMessageContaining("借贷不平衡");

        verifyNoInteractions(jdbc);
    }
}
