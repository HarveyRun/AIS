package com.shixianwen.finance;

import com.shixianwen.admin.AdminAuditLog;
import com.shixianwen.admin.AdminAuditLogRepository;
import com.shixianwen.admin.AdminManagementService;
import com.shixianwen.admin.AdminUser;
import com.shixianwen.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinanceReconciliationService {
    private final JdbcTemplate jdbc;
    private final AdminAuditLogRepository audits;
    private final AdminManagementService adminManagement;

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        return jdbc.queryForMap(
            "SELECT (SELECT COUNT(*) FROM reconciliation_differences WHERE status='OPEN') AS openDifferences," +
                "(SELECT COUNT(*) FROM reconciliation_tasks) AS taskCount," +
                "(SELECT COUNT(*) FROM fund_vouchers) AS voucherCount," +
                "(SELECT COALESCE(SUM(service_fee_amount),0) FROM platform_fee_records WHERE status='EARNED') AS earnedServiceFee," +
                "(SELECT COALESCE(SUM(channel_fee),0) FROM channel_bill_records) AS channelFee"
        );
    }

    @Transactional(readOnly = true)
    public PageResult tasks(int page, int size) {
        long total = number("SELECT COUNT(*) FROM reconciliation_tasks");
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT id,provider,bill_type AS billType,bill_date AS billDate,trigger_type AS triggerType,status," +
                "record_count AS recordCount,matched_count AS matchedCount,difference_count AS differenceCount," +
                "error_message AS errorMessage,started_at AS startedAt,completed_at AS completedAt " +
                "FROM reconciliation_tasks ORDER BY id DESC LIMIT ? OFFSET ?", size, page * size
        );
        return new PageResult(items, total, page, size);
    }

    @Transactional(readOnly = true)
    public PageResult differences(String status, int page, int size) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM reconciliation_differences WHERE (?='' OR status=?)",
            Long.class, normalized, normalized
        );
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT id,task_id AS taskId,difference_type AS differenceType,business_type AS businessType," +
                "business_id AS businessId,provider_trade_no AS providerTradeNo,expected_amount AS expectedAmount," +
                "actual_amount AS actualAmount,status,detail,resolution,created_at AS createdAt,resolved_at AS resolvedAt " +
                "FROM reconciliation_differences WHERE (?='' OR status=?) ORDER BY id DESC LIMIT ? OFFSET ?",
            normalized, normalized, size, page * size
        );
        return new PageResult(items, total, page, size);
    }

    @Transactional(readOnly = true)
    public PageResult vouchers(String keyword, int page, int size) {
        String query = keyword == null ? "" : keyword.trim();
        String like = "%" + query + "%";
        long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM fund_vouchers WHERE (?='' OR voucher_no LIKE ? OR business_id LIKE ? OR description LIKE ?)",
            Long.class, query, like, like, like
        );
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT v.id,v.voucher_no AS voucherNo,v.business_type AS businessType,v.business_id AS businessId," +
                "v.action_code AS actionCode,v.description,v.occurred_at AS occurredAt," +
                "COALESCE(SUM(ABS(e.signed_amount))/2,0) AS amount,SUM(e.signed_amount) AS balance " +
                "FROM fund_vouchers v LEFT JOIN fund_entries e ON e.voucher_id=v.id " +
                "WHERE (?='' OR v.voucher_no LIKE ? OR v.business_id LIKE ? OR v.description LIKE ?) " +
                "GROUP BY v.id ORDER BY v.id DESC LIMIT ? OFFSET ?",
            query, like, like, like, size, page * size
        );
        return new PageResult(items, total, page, size);
    }

    @Transactional
    public Map<String, Object> runInternal(AdminUser admin, String ip) {
        long taskId = createTask(admin, "INTERNAL", "ACCOUNT", LocalDate.now(), "MANUAL");
        int records = 0;
        int differences = 0;
        try {
            List<Map<String, Object>> walletErrors = jdbc.queryForList(
                "SELECT id,user_id,available_balance,recharge_balance,income_balance,frozen_balance," +
                    "frozen_recharge_balance,frozen_income_balance FROM wallet_accounts " +
                    "WHERE available_balance<>(recharge_balance+income_balance) " +
                    "OR frozen_balance<>(frozen_recharge_balance+frozen_income_balance)"
            );
            records += number("SELECT COUNT(*) FROM wallet_accounts");
            for (Map<String, Object> row : walletErrors) {
                if (addDifference(taskId, "WALLET_SOURCE_MISMATCH", "WALLET", String.valueOf(row.get("id")), null, null,
                    "用户 " + row.get("user_id") + " 的余额总额与资金来源分项不一致")) differences++;
            }

            List<Map<String, Object>> entryErrors = jdbc.queryForList(
                "SELECT v.id,v.voucher_no,SUM(e.signed_amount) balance FROM fund_vouchers v " +
                    "JOIN fund_entries e ON e.voucher_id=v.id GROUP BY v.id HAVING SUM(e.signed_amount)<>0"
            );
            records += number("SELECT COUNT(*) FROM fund_vouchers");
            for (Map<String, Object> row : entryErrors) {
                if (addDifference(taskId, "UNBALANCED_VOUCHER", "VOUCHER", String.valueOf(row.get("voucher_no")),
                    BigDecimal.ZERO, money(row.get("balance")), "资金凭证借贷不平")) differences++;
            }

            List<Map<String, Object>> holdErrors = jdbc.queryForList(
                "SELECT w.id,w.user_id,w.pending_income_balance,COALESCE(SUM(h.amount),0) hold_amount " +
                    "FROM wallet_accounts w LEFT JOIN wallet_income_holds h ON h.user_id=w.user_id " +
                    "AND h.status IN ('PENDING','DISPUTED') GROUP BY w.id " +
                    "HAVING w.pending_income_balance<>COALESCE(SUM(h.amount),0)"
            );
            records += number("SELECT COUNT(*) FROM wallet_income_holds");
            for (Map<String, Object> row : holdErrors) {
                if (addDifference(taskId, "PENDING_INCOME_MISMATCH", "WALLET", String.valueOf(row.get("id")),
                    money(row.get("hold_amount")), money(row.get("pending_income_balance")),
                    "待解冻收入与询问收入冻结明细不一致")) differences++;
            }
            finishTask(taskId, records, records - differences, differences, "COMPLETED", null);
            if (admin != null) {
                audit(admin, "RUN_RECONCILIATION", taskId, "检查 " + records + " 条，发现 " + differences + " 条差异", ip);
            }
            return Map.of("taskId", taskId, "recordCount", records, "differenceCount", differences);
        } catch (RuntimeException error) {
            finishTask(taskId, records, 0, differences, "FAILED", abbreviate(error.getMessage(), 500));
            throw error;
        }
    }

    @Scheduled(cron = "${app.finance.reconciliation-cron:0 20 2 * * *}", zone = "Asia/Shanghai")
    @Transactional
    public void scheduledInternalReconciliation() {
        runInternal(null, "SYSTEM");
    }

    @Transactional
    public Map<String, Object> importAlipayBill(AdminUser admin, MultipartFile file, LocalDate billDate, String ip) {
        if (file == null || file.isEmpty()) throw BusinessException.badRequest("请选择支付宝账单文件");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv")) throw BusinessException.badRequest("当前仅支持支付宝 CSV 账单");
        long taskId = createTask(admin, "ALIPAY", "TRADE", billDate, "IMPORT");
        int records = 0;
        int matched = 0;
        int differences = 0;
        try {
            byte[] bytes = file.getBytes();
            String content;
            try {
                content = decodeBill(bytes, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                content = decodeBill(bytes, Charset.forName("GB18030"));
            }
            if (!content.contains("商户订单号")) {
                content = decodeBill(bytes, Charset.forName("GB18030"));
            }
            List<String> lines = content.lines().toList();
            int headerIndex = findHeader(lines, "商户订单号");
            if (headerIndex < 0) throw BusinessException.badRequest("未识别到支付宝账单表头");
            List<String> headers = csv(lines.get(headerIndex));
            for (int index = headerIndex + 1; index < lines.size(); index++) {
                if (lines.get(index).isBlank()) continue;
                List<String> values = csv(lines.get(index));
                String orderNo = value(headers, values, "商户订单号");
                if (orderNo.isBlank()) continue;
                records++;
                String providerNo = value(headers, values, "支付宝交易号");
                BigDecimal gross = decimal(value(headers, values, "订单金额（元）", "订单金额(元)", "订单金额"));
                BigDecimal net = decimal(value(headers, values, "商家实收（元）", "商家实收(元)", "商家实收"));
                BigDecimal fee = decimal(value(headers, values, "服务费（元）", "服务费(元)", "服务费"));
                String raw = lines.get(index);
                jdbc.update("INSERT IGNORE INTO channel_bill_records(provider,bill_type,bill_date,source_hash," +
                        "provider_trade_no,merchant_order_no,business_type,trade_status,gross_amount,net_amount,channel_fee,raw_content) " +
                        "VALUES('ALIPAY','TRADE',?,?,?,?,?,?,?,?,?,?)",
                    billDate, sha256(raw), providerNo, orderNo, value(headers, values, "业务类型"),
                    value(headers, values, "交易状态"), gross, net, fee, raw);
                List<Map<String, Object>> recharge = jdbc.queryForList(
                    "SELECT id,amount,status,provider_trade_no FROM recharges WHERE order_no=?", orderNo
                );
                if (recharge.isEmpty()) {
                    if (addDifference(taskId, "CHANNEL_ORDER_MISSING_LOCALLY", "RECHARGE", orderNo, null, gross,
                        "支付宝账单存在，本地没有对应充值订单")) differences++;
                } else if (money(recharge.get(0).get("amount")).compareTo(gross) != 0) {
                    if (addDifference(taskId, "CHANNEL_AMOUNT_MISMATCH", "RECHARGE", orderNo,
                        money(recharge.get(0).get("amount")), gross, "充值订单金额与支付宝账单不一致")) differences++;
                } else {
                    matched++;
                }
            }
            List<Map<String, Object>> localPaidOrders = jdbc.queryForList(
                "SELECT order_no,provider_trade_no,amount FROM recharges " +
                    "WHERE status='PAID' AND DATE(paid_at)=?",
                billDate
            );
            for (Map<String, Object> order : localPaidOrders) {
                String orderNo = String.valueOf(order.get("order_no"));
                Long channelCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM channel_bill_records WHERE provider='ALIPAY' " +
                        "AND bill_type='TRADE' AND bill_date=? AND merchant_order_no=?",
                    Long.class,
                    billDate,
                    orderNo
                );
                if (channelCount == null || channelCount == 0) {
                    if (addDifference(
                        taskId,
                        "LOCAL_ORDER_MISSING_IN_CHANNEL",
                        "RECHARGE",
                        orderNo,
                        money(order.get("amount")),
                        null,
                        "本地充值已经到账，但支付宝账单中没有对应订单"
                    )) differences++;
                }
            }
            finishTask(taskId, records, matched, differences, "COMPLETED", null);
            audit(admin, "IMPORT_ALIPAY_BILL", taskId, "导入 " + records + " 条，差异 " + differences + " 条", ip);
            return Map.of("taskId", taskId, "recordCount", records, "matchedCount", matched, "differenceCount", differences);
        } catch (BusinessException error) {
            finishTask(taskId, records, matched, differences, "FAILED", abbreviate(error.getMessage(), 500));
            throw error;
        } catch (Exception error) {
            finishTask(taskId, records, matched, differences, "FAILED", abbreviate(error.getMessage(), 500));
            throw BusinessException.badRequest("支付宝账单读取失败");
        }
    }

    @Transactional
    public Map<String, Object> importWithdrawalResults(AdminUser admin, MultipartFile file, String ip) {
        if (file == null || file.isEmpty()) throw BusinessException.badRequest("请选择提现结果文件");
        int success = 0;
        int failed = 0;
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.getBytes()))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> columns = new LinkedHashMap<>();
            for (var cell : sheet.getRow(0)) columns.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex());
            Integer idColumn = first(columns, "提现编号", "ID");
            Integer resultColumn = first(columns, "处理结果", "状态");
            Integer reasonColumn = first(columns, "失败原因", "备注");
            if (idColumn == null || resultColumn == null) throw BusinessException.badRequest("文件缺少提现编号或处理结果列");
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                String idText = formatter.formatCellValue(row.getCell(idColumn)).trim();
                String result = formatter.formatCellValue(row.getCell(resultColumn)).trim();
                if (idText.isBlank() || result.isBlank()) continue;
                long id = Long.parseLong(idText.replace(".0", ""));
                String status = SetResult.success(result) ? "COMPLETED" : "FAILED";
                adminManagement.processWithdrawal(admin, id, status, ip);
                String reason = reasonColumn == null ? "" : formatter.formatCellValue(row.getCell(reasonColumn)).trim();
                jdbc.update("UPDATE withdrawals SET result_reason=?,result_imported_at=NOW(6) WHERE id=?", abbreviate(reason, 300), id);
                if ("COMPLETED".equals(status)) success++; else failed++;
            }
            audit(admin, "IMPORT_WITHDRAWAL_RESULTS", "BATCH", "成功 " + success + " 条，失败 " + failed + " 条", ip);
            return Map.of("successCount", success, "failedCount", failed);
        } catch (BusinessException error) {
            throw error;
        } catch (Exception error) {
            throw BusinessException.badRequest("提现结果文件读取失败：" + abbreviate(error.getMessage(), 120));
        }
    }

    @Transactional
    public void resolveDifference(AdminUser admin, Long id, String resolution, String ip) {
        String text = resolution == null ? "" : resolution.trim();
        if (text.isBlank()) throw BusinessException.badRequest("请填写差错处理说明");
        int updated = jdbc.update("UPDATE reconciliation_differences SET status='RESOLVED',resolution=?," +
            "resolved_by_admin_id=?,resolved_at=NOW(6) WHERE id=? AND status='OPEN'", abbreviate(text, 500), admin.getId(), id);
        if (updated == 0) throw BusinessException.badRequest("差错不存在或已经处理");
        audit(admin, "RESOLVE_RECONCILIATION_DIFFERENCE", id, text, ip);
    }

    private long createTask(AdminUser admin, String provider, String billType, LocalDate date, String trigger) {
        jdbc.update("INSERT INTO reconciliation_tasks(provider,bill_type,bill_date,trigger_type,status,started_at,created_by_admin_id) " +
            "VALUES(?,?,?,?, 'RUNNING',NOW(6),?)", provider, billType, date, trigger, admin == null ? null : admin.getId());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void finishTask(long id, int records, int matched, int differences, String status, String error) {
        jdbc.update("UPDATE reconciliation_tasks SET status=?,record_count=?,matched_count=?,difference_count=?," +
            "error_message=?,completed_at=NOW(6) WHERE id=?", status, records, matched, differences, error, id);
    }

    private boolean addDifference(long taskId, String type, String businessType, String businessId,
                                  BigDecimal expected, BigDecimal actual, String detail) {
        int inserted = jdbc.update(
            "INSERT INTO reconciliation_differences(task_id,difference_type,business_type,business_id," +
                "expected_amount,actual_amount,detail) " +
                "SELECT ?,?,?,?,?,?,? WHERE NOT EXISTS (SELECT 1 FROM reconciliation_differences " +
                "WHERE status='OPEN' AND difference_type=? AND business_type=? AND business_id=?)",
            taskId, type, businessType, businessId, expected, actual, detail,
            type, businessType, businessId
        );
        return inserted == 1;
    }

    private void audit(AdminUser admin, String action, Object targetId, String detail, String ip) {
        AdminAuditLog item = new AdminAuditLog();
        item.setAdminUser(admin);
        item.setAction(action);
        item.setTargetType("FINANCE_RECONCILIATION");
        item.setTargetId(String.valueOf(targetId));
        item.setDetail(detail);
        item.setIpAddress(ip);
        audits.save(item);
    }

    private long number(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private BigDecimal money(Object value) {
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value == null ? "0" : value));
    }

    private BigDecimal decimal(String value) {
        try { return new BigDecimal(value.replace(",", "").trim()); }
        catch (Exception ignored) { return BigDecimal.ZERO; }
    }

    private int findHeader(List<String> lines, String marker) {
        for (int i = 0; i < lines.size(); i++) if (lines.get(i).contains(marker)) return i;
        return -1;
    }

    private List<String> csv(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') quoted = !quoted;
            else if (c == ',' && !quoted) { values.add(value.toString().trim()); value.setLength(0); }
            else value.append(c);
        }
        values.add(value.toString().trim());
        return values;
    }

    private String value(List<String> headers, List<String> values, String... names) {
        for (String name : names) {
            int index = headers.indexOf(name);
            if (index >= 0 && index < values.size()) return values.get(index).trim();
        }
        return "";
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String decodeBill(byte[] bytes, Charset charset) throws Exception {
        return charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(bytes))
            .toString()
            .replace("\uFEFF", "");
    }

    private Integer first(Map<String, Integer> columns, String... names) {
        for (String name : names) if (columns.containsKey(name)) return columns.get(name);
        return null;
    }

    private String abbreviate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static final class SetResult {
        private static boolean success(String text) {
            String value = text.trim().toUpperCase(Locale.ROOT);
            return value.equals("成功") || value.equals("COMPLETED") || value.equals("SUCCESS");
        }
    }

    public record PageResult(List<Map<String, Object>> items, long total, int page, int size) {}
}
