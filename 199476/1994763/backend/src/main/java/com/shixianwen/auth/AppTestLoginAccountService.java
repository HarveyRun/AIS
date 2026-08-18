package com.shixianwen.auth;

import com.shixianwen.admin.AdminAuditLog;
import com.shixianwen.admin.AdminAuditLogRepository;
import com.shixianwen.admin.AdminUser;
import com.shixianwen.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppTestLoginAccountService {
    private final AppTestLoginAccountRepository repository;
    private final AdminAuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Optional<String> activeVerificationCode(String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        return repository.findByPhoneAndEnabledTrueAndDeletedFalse(phone)
            .map(AppTestLoginAccount::getVerificationCode)
            .filter(code -> code != null && !code.isBlank());
    }

    @Transactional(readOnly = true)
    public PageResult list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AppTestLoginAccount> result = repository.findAllByDeletedFalse(
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        return new PageResult(
            result.getContent().stream().map(View::from).toList(),
            result.getTotalElements(),
            safePage,
            safeSize
        );
    }

    @Transactional
    public View create(
        AdminUser admin,
        String phone,
        String verificationCode,
        boolean enabled,
        String ipAddress
    ) {
        String normalizedPhone = normalizePhone(phone);
        String normalizedCode = normalizeCode(verificationCode);
        AppTestLoginAccount account = repository.findByPhone(normalizedPhone)
            .filter(AppTestLoginAccount::isDeleted)
            .orElseGet(AppTestLoginAccount::new);

        if (account.getId() == null && repository.findByPhone(normalizedPhone).isPresent()) {
            throw BusinessException.badRequest("该手机号已经配置为App超级账号");
        }

        account.setPhone(normalizedPhone);
        account.setVerificationCode(normalizedCode);
        account.setEnabled(enabled);
        account.setDeleted(false);
        account.setDeletedAt(null);
        account.setUpdatedByAdmin(admin);
        account = repository.save(account);
        recordAudit(admin, "CREATE_APP_TEST_ACCOUNT", account, ipAddress);
        return View.from(account);
    }

    @Transactional
    public View update(
        AdminUser admin,
        Long id,
        String phone,
        String verificationCode,
        boolean enabled,
        String ipAddress
    ) {
        String normalizedPhone = normalizePhone(phone);
        String normalizedCode = normalizeCode(verificationCode);
        AppTestLoginAccount account = activeAccount(id);
        repository.findByPhone(normalizedPhone)
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw BusinessException.badRequest("该手机号已经配置为App超级账号");
            });

        account.setPhone(normalizedPhone);
        account.setVerificationCode(normalizedCode);
        account.setEnabled(enabled);
        account.setUpdatedByAdmin(admin);
        account = repository.save(account);
        recordAudit(admin, "UPDATE_APP_TEST_ACCOUNT", account, ipAddress);
        return View.from(account);
    }

    @Transactional
    public void delete(AdminUser admin, Long id, String ipAddress) {
        AppTestLoginAccount account = activeAccount(id);
        account.setEnabled(false);
        account.setDeleted(true);
        account.setDeletedAt(LocalDateTime.now());
        account.setUpdatedByAdmin(admin);
        repository.save(account);
        recordAudit(admin, "DELETE_APP_TEST_ACCOUNT", account, ipAddress);
    }

    private AppTestLoginAccount activeAccount(Long id) {
        return repository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> BusinessException.badRequest("App超级账号不存在"));
    }

    private void recordAudit(
        AdminUser admin,
        String action,
        AppTestLoginAccount account,
        String ipAddress
    ) {
        AdminAuditLog audit = new AdminAuditLog();
        audit.setAdminUser(admin);
        audit.setAction(action);
        audit.setTargetType("APP_TEST_ACCOUNT");
        audit.setTargetId(String.valueOf(account.getId()));
        audit.setDetail("phone=" + account.getPhone() + ", enabled=" + account.isEnabled());
        audit.setIpAddress(ipAddress);
        auditLogRepository.save(audit);
    }

    private String normalizePhone(String phone) {
        String value = phone == null ? "" : phone.trim();
        if (!value.matches("^1\\d{10}$")) {
            throw BusinessException.badRequest("请输入正确的11位手机号");
        }
        return value;
    }

    private String normalizeCode(String code) {
        String value = code == null ? "" : code.trim();
        if (!value.matches("^\\d{4}$")) {
            throw BusinessException.badRequest("验证码必须是4位数字");
        }
        return value;
    }

    public record View(
        Long id,
        String phone,
        String verificationCode,
        boolean enabled,
        String updatedBy,
        LocalDateTime updatedAt
    ) {
        static View from(AppTestLoginAccount account) {
            AdminUser admin = account.getUpdatedByAdmin();
            return new View(
                account.getId(),
                account.getPhone(),
                account.getVerificationCode(),
                account.isEnabled(),
                admin == null ? null : admin.getDisplayName(),
                account.getUpdatedAt()
            );
        }
    }

    public record PageResult(List<View> items, long total, int page, int size) {
    }
}
