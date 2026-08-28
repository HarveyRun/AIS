package com.shixianwen.wallet;

import com.shixianwen.admin.AdminAuditLog;
import com.shixianwen.admin.AdminAuditLogRepository;
import com.shixianwen.admin.AdminManagementService;
import com.shixianwen.admin.AdminUser;
import com.shixianwen.common.BusinessException;
import com.shixianwen.finance.FinancialLedgerService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.shixianwen.finance.FinancialLedgerService.entry;
import static com.shixianwen.finance.FinancialLedgerService.negative;

@Service
@RequiredArgsConstructor
public class PermanentBanPayoutService {
    private static final int MAX_BATCH_SIZE = 1000;
    private static final DateTimeFormatter BATCH_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;
    private final AccountCipher accountCipher;
    private final FinancialLedgerService ledger;
    private final AdminAuditLogRepository audits;

    @Transactional
    public void captureForPermanentBan(Long userId) {
        List<Map<String, Object>> users = jdbc.queryForList(
            "SELECT id,banned_at AS bannedAt FROM users " +
                "WHERE id=? AND account_type='NORMAL' AND account_status='SUSPENDED' " +
                "AND ban_until IS NULL FOR UPDATE",
            userId
        );
        if (users.isEmpty()) return;

        Map<String, Object> wallet = jdbc.queryForMap(
            "SELECT income_balance AS incomeBalance,recharge_balance AS rechargeBalance," +
                "frozen_balance AS frozenBalance,total_withdrawn AS totalWithdrawn " +
                "FROM wallet_accounts WHERE user_id=? FOR UPDATE",
            userId
        );
        BigDecimal amount = money(wallet.get("incomeBalance"));
        if (amount.compareTo(MoneyAmounts.ZERO) <= 0) return;

        List<Map<String, Object>> accounts = jdbc.queryForList(
            "SELECT real_name AS realName,identifier_type AS identifierType," +
                "account_ciphertext AS accountCiphertext,account_masked AS accountMasked " +
                "FROM alipay_accounts WHERE user_id=? AND authorization_type='OAUTH'",
            userId
        );
        Map<String, Object> account = accounts.isEmpty() ? null : accounts.get(0);
        String status = account == null ? "WAITING_ALIPAY" : "WAITING_EXPORT";
        LocalDateTime bannedAt = users.get(0).get("bannedAt") instanceof LocalDateTime value
            ? value
            : LocalDateTime.now();
        LocalDateTime dueAt = addBusinessDays(bannedAt, 7);

        jdbc.update(
            "UPDATE wallet_accounts SET income_balance=0.00," +
                "available_balance=recharge_balance,total_withdrawn=total_withdrawn+? WHERE user_id=?",
            amount,
            userId
        );
        jdbc.update(
            "INSERT INTO permanent_ban_payouts(" +
                "user_id,amount,payee_name_snapshot,alipay_identifier_type_snapshot," +
                "alipay_account_ciphertext_snapshot,alipay_account_masked_snapshot,status,banned_at,due_at" +
                ") VALUES(?,?,?,?,?,?,?,?,?)",
            userId,
            amount,
            account == null ? null : account.get("realName"),
            account == null ? null : account.get("identifierType"),
            account == null ? null : account.get("accountCiphertext"),
            account == null ? null : account.get("accountMasked"),
            status,
            bannedAt,
            dueAt
        );
        Long payoutId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (payoutId == null) throw BusinessException.badRequest("封禁余额处理单生成失败");
        jdbc.update(
            "INSERT INTO wallet_transactions(" +
                "user_id,transaction_type,direction,amount,available_after,frozen_after," +
                "reference_type,reference_id,description" +
                ") SELECT ?,'PERMANENT_BAN_PAYOUT','OUT',?,available_balance,frozen_balance," +
                "'PERMANENT_BAN_PAYOUT',?,'永久封禁可提现收入转入待处理' " +
                "FROM wallet_accounts WHERE user_id=?",
            userId,
            amount,
            payoutId,
            userId
        );
        ledger.record(
            "PERMANENT_BAN_PAYOUT",
            payoutId,
            "CAPTURE",
            "永久封禁可提现收入转入待处理",
            List.of(
                entry("USER_INCOME_LIABILITY", userId, amount),
                entry("PERMANENT_BAN_PAYOUT_PAYABLE", userId, negative(amount))
            )
        );
    }

    @Scheduled(fixedDelayString = "${app.wallet.permanent-ban-payout-scan-ms:60000}")
    @Transactional
    public void captureNewlyAvailableIncome() {
        List<Long> userIds = jdbc.queryForList(
            "SELECT u.id FROM users u JOIN wallet_accounts w ON w.user_id=u.id " +
                "WHERE u.account_type='NORMAL' AND u.account_status='SUSPENDED' " +
                "AND u.ban_until IS NULL AND w.income_balance>0 ORDER BY u.id LIMIT 100",
            Long.class
        );
        userIds.forEach(this::captureForPermanentBan);
    }

    public AdminManagementService.PageResult page(
        String keyword,
        String status,
        int page,
        int size
    ) {
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        String cleanStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String like = "%" + cleanKeyword + "%";
        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM permanent_ban_payouts p JOIN users u ON u.id=p.user_id " +
                "WHERE (?='' OR p.status=?) AND (?='' OR u.uid LIKE ? OR u.phone LIKE ?)",
            Long.class,
            cleanStatus,
            cleanStatus,
            cleanKeyword,
            like,
            like
        );
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT p.id,p.amount,p.status,p.batch_no AS batchNo,p.result_reason AS resultReason," +
                "p.alipay_account_masked_snapshot AS alipayAccount,p.banned_at AS bannedAt," +
                "p.due_at AS dueAt,p.exported_at AS exportedAt,p.completed_at AS completedAt," +
                "p.created_at AS createdAt,u.uid,u.phone,u.nickname " +
                "FROM permanent_ban_payouts p JOIN users u ON u.id=p.user_id " +
                "WHERE (?='' OR p.status=?) AND (?='' OR u.uid LIKE ? OR u.phone LIKE ?) " +
                "ORDER BY FIELD(p.status,'WAITING_ALIPAY','WAITING_EXPORT','FAILED','EXPORTED','COMPLETED')," +
                "p.due_at,p.id DESC LIMIT ? OFFSET ?",
            cleanStatus,
            cleanStatus,
            cleanKeyword,
            like,
            like,
            size,
            page * size
        );
        return new AdminManagementService.PageResult(items, total == null ? 0 : total, page, size);
    }

    @Transactional
    public ExportFile export(AdminUser admin, String ipAddress) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT p.id,p.amount,p.payee_name_snapshot AS payeeName," +
                "p.alipay_identifier_type_snapshot AS identifierType," +
                "p.alipay_account_ciphertext_snapshot AS accountCiphertext,p.created_at AS createdAt," +
                "u.uid FROM permanent_ban_payouts p JOIN users u ON u.id=p.user_id " +
                "WHERE p.status='WAITING_EXPORT' AND p.alipay_account_ciphertext_snapshot IS NOT NULL " +
                "ORDER BY p.due_at,p.id LIMIT " + MAX_BATCH_SIZE + " FOR UPDATE"
        );
        if (rows.isEmpty()) throw BusinessException.badRequest("暂无可导出的永久封禁余额");
        String batchNo = "SXW-PB-" + BATCH_TIME.format(LocalDateTime.now()) + "-" +
            UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        byte[] content = workbook(rows, batchNo);
        rows.forEach(row -> jdbc.update(
            "UPDATE permanent_ban_payouts SET status='EXPORTED',batch_no=?,exported_at=NOW(6) " +
                "WHERE id=? AND status='WAITING_EXPORT'",
            batchNo,
            ((Number) row.get("id")).longValue()
        ));
        audit(admin, "EXPORT_PERMANENT_BAN_PAYOUT_BATCH", batchNo, "共" + rows.size() + "笔", ipAddress);
        return new ExportFile("永久封禁余额处理-" + batchNo + ".xlsx", content, rows.size());
    }

    public ExportFile downloadBatch(String batchNo) {
        String value = batchNo == null ? "" : batchNo.trim();
        if (!value.matches("SXW-PB-[A-Z0-9-]{10,60}")) {
            throw BusinessException.badRequest("封禁余额批次号无效");
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT p.id,p.amount,p.payee_name_snapshot AS payeeName," +
                "p.alipay_identifier_type_snapshot AS identifierType," +
                "p.alipay_account_ciphertext_snapshot AS accountCiphertext,p.created_at AS createdAt," +
                "u.uid FROM permanent_ban_payouts p JOIN users u ON u.id=p.user_id " +
                "WHERE p.batch_no=? AND p.alipay_account_ciphertext_snapshot IS NOT NULL ORDER BY p.id",
            value
        );
        if (rows.isEmpty()) throw BusinessException.notFound("封禁余额批次不存在");
        return new ExportFile("永久封禁余额处理-" + value + ".xlsx", workbook(rows, value), rows.size());
    }

    @Transactional
    public Map<String, Object> importResults(AdminUser admin, MultipartFile file, String ipAddress) {
        if (file == null || file.isEmpty()) throw BusinessException.badRequest("请选择封禁余额处理结果文件");
        int success = 0;
        int failed = 0;
        int pending = 0;
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.getBytes()))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> columns = new LinkedHashMap<>();
            if (sheet.getRow(0) == null) throw BusinessException.badRequest("结果文件没有表头");
            for (var cell : sheet.getRow(0)) {
                columns.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex());
            }
            Integer idColumn = first(columns, "封禁余额处理编号", "ID");
            Integer resultColumn = first(columns, "处理结果", "状态");
            Integer reasonColumn = first(columns, "失败原因", "备注");
            if (idColumn == null || resultColumn == null) {
                throw BusinessException.badRequest("文件缺少封禁余额处理编号或处理结果列");
            }
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                String idText = formatter.formatCellValue(row.getCell(idColumn)).trim();
                String result = formatter.formatCellValue(row.getCell(resultColumn)).trim();
                if (idText.isBlank() || result.isBlank()) continue;
                long id = Long.parseLong(idText.replace(".0", ""));
                String reason = reasonColumn == null
                    ? ""
                    : formatter.formatCellValue(row.getCell(reasonColumn)).trim();
                if (isSuccess(result)) {
                    complete(id, reason);
                    success++;
                } else if (isFailure(result)) {
                    fail(id, reason);
                    failed++;
                } else if (isPending(result)) {
                    pending++;
                } else {
                    throw BusinessException.badRequest("无法识别的处理结果：" + result);
                }
            }
            audit(
                admin,
                "IMPORT_PERMANENT_BAN_PAYOUT_RESULTS",
                "BATCH",
                "成功 " + success + " 条，失败 " + failed + " 条，处理中 " + pending + " 条",
                ipAddress
            );
            return Map.of(
                "successCount", success,
                "failedCount", failed,
                "pendingCount", pending
            );
        } catch (BusinessException error) {
            throw error;
        } catch (Exception error) {
            throw BusinessException.badRequest("封禁余额结果文件读取失败");
        }
    }

    @Transactional
    public void retry(AdminUser admin, Long id, String ipAddress) {
        int affected = jdbc.update(
            "UPDATE permanent_ban_payouts SET status='WAITING_EXPORT',batch_no=NULL," +
                "exported_at=NULL,result_reason=NULL,result_imported_at=NULL " +
                "WHERE id=? AND status='FAILED' AND alipay_account_ciphertext_snapshot IS NOT NULL",
            id
        );
        if (affected != 1) throw BusinessException.badRequest("该记录当前不能重新导出");
        audit(admin, "RETRY_PERMANENT_BAN_PAYOUT", String.valueOf(id), "重新进入待导出", ipAddress);
    }

    private void complete(long id, String reason) {
        Map<String, Object> row = resultRow(id);
        jdbc.update(
            "UPDATE permanent_ban_payouts SET status='COMPLETED',result_reason=?," +
                "result_imported_at=NOW(6),completed_at=NOW(6) WHERE id=?",
            abbreviate(reason, 300),
            id
        );
        BigDecimal amount = money(row.get("amount"));
        Long userId = ((Number) row.get("userId")).longValue();
        ledger.record(
            "PERMANENT_BAN_PAYOUT",
            id,
            "COMPLETED",
            "永久封禁余额处理完成",
            List.of(
                entry("PERMANENT_BAN_PAYOUT_PAYABLE", userId, amount),
                entry("ALIPAY_CLEARING", null, negative(amount))
            )
        );
    }

    private void fail(long id, String reason) {
        resultRow(id);
        jdbc.update(
            "UPDATE permanent_ban_payouts SET status='FAILED',result_reason=?," +
                "result_imported_at=NOW(6) WHERE id=?",
            abbreviate(reason, 300),
            id
        );
    }

    private Map<String, Object> resultRow(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT user_id AS userId,amount,status FROM permanent_ban_payouts WHERE id=? FOR UPDATE",
            id
        );
        if (rows.isEmpty()) throw BusinessException.notFound("封禁余额处理记录不存在");
        Map<String, Object> row = rows.get(0);
        if (!"EXPORTED".equals(row.get("status"))) {
            throw BusinessException.badRequest("该封禁余额记录不在待回填状态");
        }
        return row;
    }

    private byte[] workbook(List<Map<String, Object>> source, String batchNo) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("永久封禁余额处理");
            String[] headers = {
                "批次号", "封禁余额处理编号", "用户UID", "收款人姓名", "支付宝标识类型",
                "支付宝账户标识", "金额", "生成时间"
            };
            Row title = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                title.createCell(index).setCellValue(headers[index]);
            }
            for (int index = 0; index < source.size(); index++) {
                Map<String, Object> item = source.get(index);
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(batchNo);
                row.createCell(1).setCellValue(((Number) item.get("id")).longValue());
                row.createCell(2).setCellValue(String.valueOf(item.get("uid")));
                row.createCell(3).setCellValue(text(item.get("payeeName")));
                row.createCell(4).setCellValue(text(item.get("identifierType")));
                row.createCell(5).setCellValue(accountCipher.decrypt(text(item.get("accountCiphertext"))));
                row.createCell(6).setCellValue(money(item.get("amount")).doubleValue());
                Object createdAt = item.get("createdAt");
                row.createCell(7).setCellValue(
                    createdAt instanceof LocalDateTime value
                        ? DISPLAY_TIME.format(value)
                        : text(createdAt)
                );
            }
            int[] widths = {30, 20, 16, 16, 18, 32, 14, 22};
            for (int index = 0; index < widths.length; index++) {
                sheet.setColumnWidth(index, widths[index] * 256);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception error) {
            if (error instanceof BusinessException businessException) throw businessException;
            throw BusinessException.badRequest("封禁余额表格生成失败");
        }
    }

    private LocalDateTime addBusinessDays(LocalDateTime start, int days) {
        LocalDateTime result = start;
        int added = 0;
        while (added < days) {
            result = result.plusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return result;
    }

    private Integer first(Map<String, Integer> columns, String... names) {
        for (String name : names) {
            if (columns.containsKey(name)) return columns.get(name);
        }
        return null;
    }

    private boolean isSuccess(String result) {
        String value = result.trim().toUpperCase(Locale.ROOT);
        return List.of("SUCCESS", "COMPLETED", "成功", "已完成", "已到账").contains(value);
    }

    private boolean isFailure(String result) {
        String value = result.trim().toUpperCase(Locale.ROOT);
        return List.of("FAIL", "FAILED", "失败", "已失败").contains(value);
    }

    private boolean isPending(String result) {
        String value = result.trim().toUpperCase(Locale.ROOT);
        return List.of("PENDING", "PROCESSING", "处理中", "待处理").contains(value);
    }

    private BigDecimal money(Object value) {
        if (value instanceof BigDecimal amount) return MoneyAmounts.normalize(amount);
        return MoneyAmounts.normalize(new BigDecimal(String.valueOf(value)));
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String abbreviate(String value, int maximum) {
        String result = value == null ? "" : value.trim();
        return result.length() <= maximum ? result : result.substring(0, maximum);
    }

    private void audit(AdminUser admin, String action, String targetId, String detail, String ipAddress) {
        AdminAuditLog audit = new AdminAuditLog();
        audit.setAdminUser(admin);
        audit.setAction(action);
        audit.setTargetType("PERMANENT_BAN_PAYOUT");
        audit.setTargetId(targetId);
        audit.setDetail(detail);
        audit.setIpAddress(ipAddress);
        audits.save(audit);
    }

    public record ExportFile(String filename, byte[] content, int count) {
        public String encodedFilename() {
            return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        }
    }
}
