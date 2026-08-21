package com.shixianwen.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public boolean hasPermission(AdminUser admin, String permissionCode) {
        if (admin == null || permissionCode == null || permissionCode.isBlank()) return false;
        if (isSuperAdmin(admin.getId())) return true;
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_user_roles ur " +
                "JOIN admin_roles r ON r.id=ur.role_id AND r.active=TRUE AND r.deleted_at IS NULL " +
                "JOIN admin_role_permissions rp ON rp.role_id=r.id " +
                "JOIN admin_permissions p ON p.id=rp.permission_id AND p.active=TRUE AND p.deleted_at IS NULL " +
                "WHERE ur.admin_user_id=? AND p.code=?",
            Long.class,
            admin.getId(), permissionCode
        );
        return count != null && count > 0;
    }

    @Transactional(readOnly = true)
    public Set<String> permissionCodes(Long adminUserId) {
        if (isSuperAdmin(adminUserId)) return Set.of("*");
        return new LinkedHashSet<>(jdbc.queryForList(
            "SELECT p.code FROM admin_user_roles ur " +
                "JOIN admin_roles r ON r.id=ur.role_id AND r.active=TRUE AND r.deleted_at IS NULL " +
                "JOIN admin_role_permissions rp ON rp.role_id=r.id " +
                "JOIN admin_permissions p ON p.id=rp.permission_id AND p.active=TRUE AND p.deleted_at IS NULL " +
                "WHERE ur.admin_user_id=? GROUP BY p.code ORDER BY MIN(p.sort_order),MIN(p.id)",
            String.class,
            adminUserId
        ));
    }

    @Transactional(readOnly = true)
    public List<AdminRoleSummary> roles(Long adminUserId) {
        return jdbc.query(
            "SELECT r.id,r.code,r.name,r.level_no FROM admin_user_roles ur " +
                "JOIN admin_roles r ON r.id=ur.role_id AND r.active=TRUE AND r.deleted_at IS NULL " +
                "WHERE ur.admin_user_id=? ORDER BY r.level_no,r.id",
            (result, row) -> new AdminRoleSummary(
                result.getLong("id"), result.getString("code"), result.getString("name"),
                result.getInt("level_no")
            ),
            adminUserId
        );
    }

    @Transactional(readOnly = true)
    public boolean isSuperAdmin(Long adminUserId) {
        if (adminUserId == null) return false;
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_user_roles ur JOIN admin_roles r ON r.id=ur.role_id " +
                "WHERE ur.admin_user_id=? AND r.code='SUPER_ADMIN' AND r.active=TRUE AND r.deleted_at IS NULL",
            Long.class,
            adminUserId
        );
        return count != null && count > 0;
    }

    public record AdminRoleSummary(Long id, String code, String name, int level) {
    }
}
