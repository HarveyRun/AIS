package com.shixianwen.admin;

import com.shixianwen.common.BusinessException;
import com.shixianwen.wallet.AccountCipher;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WithdrawalBatchExportService {
    private static final int MAX_BATCH_SIZE = 1000;
    private static final DateTimeFormatter BATCH_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;
    private final AccountCipher accountCipher;
    private final AdminAuditLogRepository audits;

    @Transactional
    public ExportFile export(AdminUser admin, String ipAddress) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT w.id,w.amount,w.payee_name_snapshot AS payeeName," +
                "w.alipay_identifier_type_snapshot AS identifierType," +
                "w.alipay_account_ciphertext_snapshot AS accountCiphertext,w.created_at AS createdAt," +
                "u.uid FROM withdrawals w JOIN users u ON u.id=w.user_id " +
                "WHERE w.status='PROCESSING' AND u.account_type='NORMAL' " +
                "AND w.alipay_account_ciphertext_snapshot IS NOT NULL " +
                "ORDER BY w.id LIMIT " + MAX_BATCH_SIZE + " FOR UPDATE"
        );
        if (rows.isEmpty()) throw BusinessException.badRequest("暂无可导出的提现申请");

        String batchNo = "SXW-WD-" + BATCH_TIME.format(LocalDateTime.now()) + "-" +
            UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        byte[] content = workbook(rows, batchNo);

        List<Long> ids = rows.stream()
            .map(row -> ((Number) row.get("id")).longValue())
            .toList();
        ids.forEach(id -> jdbc.update(
            "UPDATE withdrawals SET status='EXPORTED',batch_no=?,exported_at=NOW(6) " +
                "WHERE id=? AND status='PROCESSING'",
            batchNo,
            id
        ));

        AdminAuditLog audit = new AdminAuditLog();
        audit.setAdminUser(admin);
        audit.setAction("EXPORT_WITHDRAWAL_BATCH");
        audit.setTargetType("WITHDRAWAL_BATCH");
        audit.setTargetId(batchNo);
        audit.setDetail("共" + rows.size() + "笔");
        audit.setIpAddress(ipAddress);
        audits.save(audit);

        return new ExportFile(batchNo + ".xlsx", content, rows.size());
    }

    public ExportFile downloadBatch(String batchNo) {
        String normalizedBatchNo = batchNo == null ? "" : batchNo.trim();
        if (!normalizedBatchNo.matches("SXW-WD-[A-Z0-9-]{10,60}")) {
            throw BusinessException.badRequest("提现批次号无效");
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT w.id,w.amount,w.payee_name_snapshot AS payeeName," +
                "w.alipay_identifier_type_snapshot AS identifierType," +
                "w.alipay_account_ciphertext_snapshot AS accountCiphertext,w.created_at AS createdAt," +
                "u.uid FROM withdrawals w JOIN users u ON u.id=w.user_id " +
                "WHERE w.batch_no=? AND w.alipay_account_ciphertext_snapshot IS NOT NULL ORDER BY w.id",
            normalizedBatchNo
        );
        if (rows.isEmpty()) throw BusinessException.notFound("提现批次不存在");
        return new ExportFile(normalizedBatchNo + ".xlsx", workbook(rows, normalizedBatchNo), rows.size());
    }

    private byte[] workbook(List<Map<String, Object>> source, String batchNo) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("支付宝提现");
            CellStyle centered = workbook.createCellStyle();
            centered.setAlignment(HorizontalAlignment.CENTER);

            Row title = sheet.createRow(0);
            String[] headers = {
                "批次号", "提现编号", "用户UID", "收款人姓名", "支付宝标识类型",
                "支付宝账户标识", "金额", "申请时间"
            };
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
                row.createCell(5).setCellValue(accountCipher.decrypt(String.valueOf(item.get("accountCiphertext"))));
                row.createCell(6).setCellValue(((BigDecimal) item.get("amount")).doubleValue());
                Object createdAt = item.get("createdAt");
                row.createCell(7).setCellValue(
                    createdAt instanceof LocalDateTime time ? DISPLAY_TIME.format(time) : String.valueOf(createdAt)
                );
                row.getCell(1).setCellStyle(centered);
                row.getCell(6).setCellStyle(centered);
            }

            int[] widths = {30, 14, 16, 16, 18, 32, 14, 22};
            for (int index = 0; index < widths.length; index++) {
                sheet.setColumnWidth(index, widths[index] * 256);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            if (exception instanceof BusinessException businessException) throw businessException;
            throw BusinessException.badRequest("提现表格生成失败，请稍后重试");
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public record ExportFile(String filename, byte[] content, int count) {
    }
}
