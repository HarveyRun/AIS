package com.shixianwen.finance;

import com.shixianwen.common.BusinessException;
import com.shixianwen.wallet.MoneyAmounts;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialLedgerService {
    private final JdbcTemplate jdbc;

    public void record(
        String businessType,
        Object businessId,
        String actionCode,
        String description,
        List<Entry> entries
    ) {
        if (entries == null || entries.size() < 2) {
            throw BusinessException.badRequest("资金凭证明细不完整");
        }
        BigDecimal total = entries.stream()
            .map(Entry::signedAmount)
            .map(MoneyAmounts::normalize)
            .reduce(MoneyAmounts.ZERO, MoneyAmounts::add);
        if (!MoneyAmounts.same(total, MoneyAmounts.ZERO)) {
            throw BusinessException.badRequest("资金凭证借贷不平衡");
        }

        String normalizedBusinessId = String.valueOf(businessId);
        int inserted = jdbc.update(
            "INSERT IGNORE INTO fund_vouchers(" +
                "voucher_no,business_type,business_id,action_code,description,occurred_at" +
                ") VALUES (?,?,?,?,?,?)",
            "FV-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(),
            businessType,
            normalizedBusinessId,
            actionCode,
            description,
            LocalDateTime.now()
        );
        if (inserted == 0) return;

        Long voucherId = jdbc.queryForObject(
            "SELECT id FROM fund_vouchers WHERE business_type=? AND business_id=? AND action_code=?",
            Long.class,
            businessType,
            normalizedBusinessId,
            actionCode
        );
        if (voucherId == null) throw BusinessException.badRequest("资金凭证生成失败");
        for (Entry entry : entries) {
            jdbc.update(
                "INSERT INTO fund_entries(voucher_id,account_code,user_id,signed_amount) VALUES (?,?,?,?)",
                voucherId,
                entry.accountCode(),
                entry.userId(),
                MoneyAmounts.normalize(entry.signedAmount())
            );
        }
    }

    public static Entry entry(String accountCode, Long userId, BigDecimal signedAmount) {
        return new Entry(accountCode, userId, signedAmount);
    }

    public static BigDecimal negative(BigDecimal amount) {
        return MoneyAmounts.normalize(amount).negate();
    }

    public record Entry(String accountCode, Long userId, BigDecimal signedAmount) {
    }
}
