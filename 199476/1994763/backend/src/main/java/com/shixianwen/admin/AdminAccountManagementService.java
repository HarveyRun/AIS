package com.shixianwen.admin;

import com.shixianwen.common.BusinessException;
import com.shixianwen.security.SecurityEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminAccountManagementService {
    public static final String DEFAULT_RESET_PASSWORD = "123456abcAbc";

    private final JdbcTemplate jdbc;
    private final AdminUserRepository users;
    private final AdminSessionRepository sessions;
    private final AdminPasswordEncoder passwords;
    private final AdminAuditLogRepository audits;
    private final SecurityEventService securityEvents;
    private final AdminAuthorizationService authorization;

    @Transactional(readOnly = true)
    public PageResult users(String keyword, int page, int size) {
        String query = clean(keyword);
        String like = "%" + query + "%";
        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_users u WHERE u.deleted_at IS NULL " +
                "AND (?='' OR u.phone LIKE ? OR u.display_name LIKE ?)",
            Long.class, query, like, like
        );
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT u.id,u.phone,u.display_name AS displayName,u.role,u.status," +
                "u.last_login_at AS lastLoginAt,u.created_at AS createdAt " +
                "FROM admin_users u WHERE u.deleted_at IS NULL " +
                "AND (?='' OR u.phone LIKE ? OR u.display_name LIKE ?) " +
                "ORDER BY u.id LIMIT ? OFFSET ?",
            query, like, like, size, page * size
        );
        items.forEach(item -> item.put("roles", userRoles(number(item.get("id")))));
        return new PageResult(items, total == null ? 0 : total, page, size);
    }

    @Transactional
    public Map<String, Object> createUser(
        AdminUser operator,
        String phone,
        String displayName,
        String password,
        String status,
        List<Long> roleIds,
        String ipAddress
    ) {
        requirePermission(operator, "ADMIN_USER_ASSIGN_ROLE");
        String normalizedPhone = phone(phone);
        ensurePhoneAvailable(normalizedPhone, null);
        List<Long> normalizedRoles = requireRoles(roleIds);
        ensureRolesAssignable(operator, normalizedRoles);
        AdminUser user = new AdminUser();
        user.setPhone(normalizedPhone);
        user.setDisplayName(required(displayName, "请输入管理员名称", 60));
        user.setPasswordHash(passwords.encode(validPassword(password, true)));
        user.setMustChangePassword(true);
        user.setStatus(status(status));
        user.setRole(primaryRoleCode(normalizedRoles));
        user = users.saveAndFlush(user);
        assignRoles(user.getId(), normalizedRoles);
        audit(operator, "CREATE_ADMIN_USER", "ADMIN_USER", user.getId(), user.getPhone(), ipAddress);
        return userView(user.getId());
    }

    @Transactional
    public Map<String, Object> updateUser(
        AdminUser operator,
        Long id,
        String phone,
        String displayName,
        String status,
        String ipAddress
    ) {
        AdminUser user = activeUser(id);
        ensureUserManageable(operator, id, true);
        String normalizedPhone = phone(phone);
        ensurePhoneAvailable(normalizedPhone, id);
        String normalizedStatus = status(status);
        List<Long> currentRoles = userRoles(id).stream()
            .map(role -> number(role.get("id")))
            .toList();
        if (Objects.equals(operator.getId(), id) && !"ACTIVE".equals(normalizedStatus)) {
            throw BusinessException.badRequest("不能停用当前登录账号");
        }
        protectLastSuperAdmin(id, currentRoles, normalizedStatus);
        user.setPhone(normalizedPhone);
        user.setDisplayName(required(displayName, "请输入管理员名称", 60));
        user.setStatus(normalizedStatus);
        users.save(user);
        if (!"ACTIVE".equals(normalizedStatus)) sessions.deleteByAdminUserId(id);
        audit(operator, "UPDATE_ADMIN_USER", "ADMIN_USER", id, user.getPhone(), ipAddress);
        return userView(id);
    }

    @Transactional
    public void assignUserRoles(AdminUser operator, Long id, List<Long> roleIds, String ipAddress) {
        AdminUser user = activeUser(id);
        ensureUserManageable(operator, id, false);
        List<Long> normalizedRoles = requireRoles(roleIds);
        ensureRolesAssignable(operator, normalizedRoles);
        protectLastSuperAdmin(id, normalizedRoles, user.getStatus());
        assignRoles(id, normalizedRoles);
        user.setRole(primaryRoleCode(normalizedRoles));
        users.save(user);
        sessions.deleteByAdminUserId(id);
        audit(operator, "ASSIGN_ADMIN_ROLES", "ADMIN_USER", id, normalizedRoles.toString(), ipAddress);
    }

    @Transactional
    public void deleteUser(AdminUser operator, Long id, String ipAddress) {
        if (Objects.equals(operator.getId(), id)) throw BusinessException.badRequest("不能删除当前登录账号");
        AdminUser user = activeUser(id);
        ensureUserManageable(operator, id, false);
        protectLastSuperAdmin(id, List.of(), "DELETED");
        user.setStatus("DELETED");
        user.setDeletedAt(LocalDateTime.now());
        users.save(user);
        sessions.deleteByAdminUserId(id);
        audit(operator, "DELETE_ADMIN_USER", "ADMIN_USER", id, "SOFT_DELETE", ipAddress);
    }

    @Transactional
    public void resetPassword(AdminUser operator, Long targetId, String ipAddress) {
        AdminUser target = activeUser(targetId);
        ensureUserManageable(operator, targetId, true);
        target.setPasswordHash(passwords.encode(DEFAULT_RESET_PASSWORD));
        target.setMustChangePassword(true);
        users.save(target);
        sessions.deleteByAdminUserId(targetId);
        audit(operator, "RESET_ADMIN_PASSWORD", "ADMIN_USER", targetId, "重置为默认密码", ipAddress);
        securityEvents.recordSafely(
            null, operator.getId(), "ADMIN_PASSWORD_RESET", "CRITICAL", ipAddress, null,
            "targetAdminId=" + targetId
        );
    }

    @Transactional(readOnly = true)
    public PageResult roles(String keyword, int page, int size) {
        String query = clean(keyword);
        String like = "%" + query + "%";
        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_roles WHERE deleted_at IS NULL " +
                "AND (?='' OR code LIKE ? OR name LIKE ?)",
            Long.class, query, like, like
        );
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT r.id,r.code,r.name,r.level_no AS level,r.description,r.system_role AS systemRole," +
                "r.active,r.created_at AS createdAt," +
                "(SELECT COUNT(*) FROM admin_user_roles ur JOIN admin_users u ON u.id=ur.admin_user_id " +
                " WHERE ur.role_id=r.id AND u.deleted_at IS NULL) AS userCount " +
                "FROM admin_roles r WHERE r.deleted_at IS NULL " +
                "AND (?='' OR r.code LIKE ? OR r.name LIKE ?) ORDER BY r.level_no,r.id LIMIT ? OFFSET ?",
            query, like, like, size, page * size
        );
        items.forEach(item -> item.put("permissionIds", rolePermissionIds(number(item.get("id")))));
        return new PageResult(items, total == null ? 0 : total, page, size);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> roleOptions(AdminUser operator) {
        if (authorization.isSuperAdmin(operator.getId())) {
            return jdbc.queryForList(
                "SELECT id,code,name,level_no AS level FROM admin_roles " +
                    "WHERE active=TRUE AND deleted_at IS NULL ORDER BY level_no,id"
            );
        }
        return jdbc.queryForList(
            "SELECT id,code,name,level_no AS level FROM admin_roles " +
                "WHERE active=TRUE AND deleted_at IS NULL AND level_no>? " +
                "AND NOT EXISTS (" +
                " SELECT 1 FROM admin_role_permissions target_rp " +
                " JOIN admin_permissions target_p ON target_p.id=target_rp.permission_id " +
                "  AND target_p.active=TRUE AND target_p.deleted_at IS NULL " +
                " WHERE target_rp.role_id=admin_roles.id AND NOT EXISTS (" +
                "  SELECT 1 FROM admin_user_roles own_ur " +
                "  JOIN admin_roles own_r ON own_r.id=own_ur.role_id " +
                "   AND own_r.active=TRUE AND own_r.deleted_at IS NULL " +
                "  JOIN admin_role_permissions own_rp ON own_rp.role_id=own_r.id " +
                "  WHERE own_ur.admin_user_id=? AND own_rp.permission_id=target_rp.permission_id" +
                " )) ORDER BY level_no,id",
            operatorLevel(operator), operator.getId()
        );
    }

    @Transactional
    public Map<String, Object> createRole(
        AdminUser operator,
        String code,
        String name,
        Integer level,
        String description,
        Boolean active,
        List<Long> permissionIds,
        String ipAddress
    ) {
        if (permissionIds != null && !permissionIds.isEmpty()) {
            requirePermission(operator, "ROLE_ASSIGN_PERMISSION");
        }
        String normalizedCode = roleCode(code);
        List<Long> normalizedPermissions = requirePermissions(permissionIds);
        ensurePermissionsAssignable(operator, normalizedPermissions);
        ensureRoleLevelManageable(operator, safeLevel(level));
        ensureRoleCodeAvailable(normalizedCode, null);
        jdbc.update(
            "INSERT INTO admin_roles(code,name,level_no,description,system_role,active) VALUES (?,?,?,?,FALSE,?)",
            normalizedCode, required(name, "请输入角色名称", 80), safeLevel(level), optional(description, 300),
            active == null || active
        );
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        assignPermissions(id, normalizedPermissions);
        audit(operator, "CREATE_ADMIN_ROLE", "ADMIN_ROLE", id, normalizedCode, ipAddress);
        return roleView(id);
    }

    @Transactional
    public Map<String, Object> updateRole(
        AdminUser operator,
        Long id,
        String code,
        String name,
        Integer level,
        String description,
        Boolean active,
        String ipAddress
    ) {
        Map<String, Object> existing = roleRow(id);
        ensureRoleManageable(operator, existing);
        ensureRolePermissionsManageable(operator, id);
        String existingCode = String.valueOf(existing.get("code"));
        String normalizedCode = roleCode(code);
        ensureRoleLevelManageable(operator, safeLevel(level));
        if (Boolean.TRUE.equals(existing.get("systemRole")) && !existingCode.equals(normalizedCode)) {
            throw BusinessException.badRequest("系统角色编码不能修改");
        }
        if ("SUPER_ADMIN".equals(existingCode) && Boolean.FALSE.equals(active)) {
            throw BusinessException.badRequest("超级管理员角色不能停用");
        }
        ensureRoleCodeAvailable(normalizedCode, id);
        jdbc.update(
            "UPDATE admin_roles SET code=?,name=?,level_no=?,description=?,active=? WHERE id=? AND deleted_at IS NULL",
            normalizedCode, required(name, "请输入角色名称", 80), safeLevel(level), optional(description, 300),
            active == null || active, id
        );
        refreshPrimaryRolesForRole(id);
        expireSessionsForRole(id);
        audit(operator, "UPDATE_ADMIN_ROLE", "ADMIN_ROLE", id, normalizedCode, ipAddress);
        return roleView(id);
    }

    @Transactional
    public void assignRolePermissions(
        AdminUser operator,
        Long id,
        List<Long> permissionIds,
        String ipAddress
    ) {
        Map<String, Object> role = roleRow(id);
        ensureRoleManageable(operator, role);
        ensureRolePermissionsManageable(operator, id);
        if ("SUPER_ADMIN".equals(role.get("code"))) {
            throw BusinessException.badRequest("超级管理员始终拥有全部权限，无需单独配置");
        }
        List<Long> normalized = requirePermissions(permissionIds);
        ensurePermissionsAssignable(operator, normalized);
        assignPermissions(id, normalized);
        expireSessionsForRole(id);
        audit(operator, "ASSIGN_ROLE_PERMISSIONS", "ADMIN_ROLE", id, normalized.toString(), ipAddress);
    }

    @Transactional
    public void deleteRole(AdminUser operator, Long id, String ipAddress) {
        Map<String, Object> role = roleRow(id);
        ensureRoleManageable(operator, role);
        ensureRolePermissionsManageable(operator, id);
        if (Boolean.TRUE.equals(role.get("systemRole"))) {
            throw BusinessException.badRequest("系统预置角色不能删除");
        }
        Long usersWithRole = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_user_roles ur JOIN admin_users u ON u.id=ur.admin_user_id " +
                "WHERE ur.role_id=? AND u.deleted_at IS NULL",
            Long.class, id
        );
        if (usersWithRole != null && usersWithRole > 0) {
            throw BusinessException.badRequest("该角色仍关联后台账号，请先解除关联");
        }
        jdbc.update("DELETE FROM admin_role_permissions WHERE role_id=?", id);
        jdbc.update("UPDATE admin_roles SET active=FALSE,deleted_at=NOW(6) WHERE id=?", id);
        audit(operator, "DELETE_ADMIN_ROLE", "ADMIN_ROLE", id, "SOFT_DELETE", ipAddress);
    }

    @Transactional(readOnly = true)
    public PageResult permissions(String keyword, String module, int page, int size) {
        String query = clean(keyword);
        String normalizedModule = clean(module);
        String like = "%" + query + "%";
        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_permissions WHERE deleted_at IS NULL " +
                "AND (?='' OR code LIKE ? OR name LIKE ?) AND (?='' OR module_name=?)",
            Long.class, query, like, like, normalizedModule, normalizedModule
        );
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT id,code,name,module_name AS moduleName,action_name AS actionName,sort_order AS sortOrder," +
                "system_permission AS systemPermission,active,created_at AS createdAt " +
                "FROM admin_permissions WHERE deleted_at IS NULL " +
                "AND (?='' OR code LIKE ? OR name LIKE ?) AND (?='' OR module_name=?) " +
                "ORDER BY sort_order,id LIMIT ? OFFSET ?",
            query, like, like, normalizedModule, normalizedModule, size, page * size
        );
        return new PageResult(items, total == null ? 0 : total, page, size);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> permissionOptions(AdminUser operator) {
        if (authorization.isSuperAdmin(operator.getId())) {
            return jdbc.queryForList(
                "SELECT id,code,name,module_name AS moduleName,action_name AS actionName " +
                    "FROM admin_permissions WHERE active=TRUE AND deleted_at IS NULL ORDER BY sort_order,id"
            );
        }
        return jdbc.queryForList(
            "SELECT DISTINCT p.id,p.code,p.name,p.module_name AS moduleName,p.action_name AS actionName,p.sort_order " +
                "FROM admin_user_roles ur JOIN admin_role_permissions rp ON rp.role_id=ur.role_id " +
                "JOIN admin_permissions p ON p.id=rp.permission_id AND p.active=TRUE AND p.deleted_at IS NULL " +
                "WHERE ur.admin_user_id=? ORDER BY p.sort_order,p.id",
            operator.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<String> permissionModules() {
        return jdbc.queryForList(
            "SELECT DISTINCT module_name FROM admin_permissions WHERE deleted_at IS NULL ORDER BY module_name",
            String.class
        );
    }

    @Transactional
    public Map<String, Object> createPermission(
        AdminUser operator,
        String code,
        String name,
        String moduleName,
        String actionName,
        Integer sortOrder,
        Boolean active,
        String ipAddress
    ) {
        String normalizedCode = permissionCode(code);
        ensurePermissionCodeAvailable(normalizedCode, null);
        jdbc.update(
            "INSERT INTO admin_permissions(code,name,module_name,action_name,sort_order,system_permission,active) " +
                "VALUES (?,?,?,?,?,FALSE,?)",
            normalizedCode, required(name, "请输入权限名称", 100),
            required(moduleName, "请输入所属模块", 60), required(actionName, "请输入操作名称", 60),
            sortOrder == null ? 0 : sortOrder, active == null || active
        );
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        audit(operator, "CREATE_ADMIN_PERMISSION", "ADMIN_PERMISSION", id, normalizedCode, ipAddress);
        return permissionView(id);
    }

    @Transactional
    public Map<String, Object> updatePermission(
        AdminUser operator,
        Long id,
        String code,
        String name,
        String moduleName,
        String actionName,
        Integer sortOrder,
        Boolean active,
        String ipAddress
    ) {
        Map<String, Object> existing = permissionRow(id);
        String existingCode = String.valueOf(existing.get("code"));
        String normalizedCode = permissionCode(code);
        if (Boolean.TRUE.equals(existing.get("systemPermission")) && !existingCode.equals(normalizedCode)) {
            throw BusinessException.badRequest("系统权限编码不能修改");
        }
        ensurePermissionCodeAvailable(normalizedCode, id);
        jdbc.update(
            "UPDATE admin_permissions SET code=?,name=?,module_name=?,action_name=?,sort_order=?,active=? " +
                "WHERE id=? AND deleted_at IS NULL",
            normalizedCode, required(name, "请输入权限名称", 100),
            required(moduleName, "请输入所属模块", 60), required(actionName, "请输入操作名称", 60),
            sortOrder == null ? 0 : sortOrder, active == null || active, id
        );
        expireSessionsForPermission(id);
        audit(operator, "UPDATE_ADMIN_PERMISSION", "ADMIN_PERMISSION", id, normalizedCode, ipAddress);
        return permissionView(id);
    }

    @Transactional
    public void deletePermission(AdminUser operator, Long id, String ipAddress) {
        Map<String, Object> permission = permissionRow(id);
        if (Boolean.TRUE.equals(permission.get("systemPermission"))) {
            throw BusinessException.badRequest("系统权限不能删除");
        }
        expireSessionsForPermission(id);
        jdbc.update("DELETE FROM admin_role_permissions WHERE permission_id=?", id);
        jdbc.update("UPDATE admin_permissions SET active=FALSE,deleted_at=NOW(6) WHERE id=?", id);
        audit(operator, "DELETE_ADMIN_PERMISSION", "ADMIN_PERMISSION", id, "SOFT_DELETE", ipAddress);
    }

    private Map<String, Object> userView(Long id) {
        Map<String, Object> item = jdbc.queryForMap(
            "SELECT id,phone,display_name AS displayName,role,status,last_login_at AS lastLoginAt," +
                "created_at AS createdAt FROM admin_users WHERE id=? AND deleted_at IS NULL",
            id
        );
        item.put("roles", userRoles(id));
        return item;
    }

    private Map<String, Object> roleView(Long id) {
        Map<String, Object> item = roleRow(id);
        item.put("permissionIds", rolePermissionIds(id));
        return item;
    }

    private Map<String, Object> permissionView(Long id) {
        return permissionRow(id);
    }

    private List<Map<String, Object>> userRoles(Long userId) {
        return jdbc.queryForList(
            "SELECT r.id,r.code,r.name,r.level_no AS level FROM admin_user_roles ur " +
                "JOIN admin_roles r ON r.id=ur.role_id AND r.deleted_at IS NULL " +
                "WHERE ur.admin_user_id=? ORDER BY r.level_no,r.id",
            userId
        );
    }

    private List<Long> rolePermissionIds(Long roleId) {
        return jdbc.queryForList(
            "SELECT permission_id FROM admin_role_permissions WHERE role_id=? ORDER BY permission_id",
            Long.class,
            roleId
        );
    }

    private void assignRoles(Long userId, List<Long> roleIds) {
        jdbc.update("DELETE FROM admin_user_roles WHERE admin_user_id=?", userId);
        roleIds.forEach(roleId -> jdbc.update(
            "INSERT INTO admin_user_roles(admin_user_id,role_id) VALUES (?,?)", userId, roleId
        ));
        sessions.deleteByAdminUserId(userId);
    }

    private void assignPermissions(Long roleId, List<Long> permissionIds) {
        jdbc.update("DELETE FROM admin_role_permissions WHERE role_id=?", roleId);
        permissionIds.forEach(permissionId -> jdbc.update(
            "INSERT INTO admin_role_permissions(role_id,permission_id) VALUES (?,?)", roleId, permissionId
        ));
    }

    private List<Long> requireRoles(List<Long> roleIds) {
        List<Long> ids = distinctIds(roleIds);
        if (ids.isEmpty()) throw BusinessException.badRequest("请至少选择一个角色");
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_roles WHERE id IN (" + placeholders(ids.size()) + ") " +
                "AND active=TRUE AND deleted_at IS NULL",
            Long.class,
            ids.toArray()
        );
        if (count == null || count != ids.size()) throw BusinessException.badRequest("选择的角色不存在或已停用");
        return ids;
    }

    private List<Long> requirePermissions(List<Long> permissionIds) {
        List<Long> ids = distinctIds(permissionIds);
        if (ids.isEmpty()) return ids;
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_permissions WHERE id IN (" + placeholders(ids.size()) + ") " +
                "AND active=TRUE AND deleted_at IS NULL",
            Long.class,
            ids.toArray()
        );
        if (count == null || count != ids.size()) throw BusinessException.badRequest("选择的权限不存在或已停用");
        return ids;
    }

    private List<Long> distinctIds(List<Long> source) {
        if (source == null) return List.of();
        List<Long> result = new ArrayList<>();
        source.stream().filter(Objects::nonNull).distinct().forEach(result::add);
        return result;
    }

    private void requirePermission(AdminUser operator, String code) {
        if (!authorization.hasPermission(operator, code)) {
            throw BusinessException.forbidden("没有权限执行此操作");
        }
    }

    private void ensureUserManageable(AdminUser operator, Long targetId, boolean allowSelf) {
        if (Objects.equals(operator.getId(), targetId)) {
            if (allowSelf) return;
            throw BusinessException.forbidden("不能修改当前账号的角色");
        }
        if (authorization.isSuperAdmin(operator.getId())) return;
        Integer targetLevel = jdbc.queryForObject(
            "SELECT MIN(r.level_no) FROM admin_user_roles ur JOIN admin_roles r ON r.id=ur.role_id " +
                "WHERE ur.admin_user_id=? AND r.active=TRUE AND r.deleted_at IS NULL",
            Integer.class,
            targetId
        );
        if (targetLevel == null || targetLevel <= operatorLevel(operator)) {
            throw BusinessException.forbidden("不能管理同级或更高级别的管理员");
        }
    }

    private void ensureRolesAssignable(AdminUser operator, List<Long> roleIds) {
        if (authorization.isSuperAdmin(operator.getId())) return;
        int level = operatorLevel(operator);
        Long forbidden = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_roles WHERE id IN (" + placeholders(roleIds.size()) + ") " +
                "AND level_no<=? AND deleted_at IS NULL",
            Long.class,
            append(roleIds, level)
        );
        if (forbidden != null && forbidden > 0) {
            throw BusinessException.forbidden("不能分配同级或更高级别的角色");
        }
        Long unavailablePermissions = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT target_rp.permission_id) FROM admin_role_permissions target_rp " +
                "JOIN admin_permissions target_p ON target_p.id=target_rp.permission_id " +
                " AND target_p.active=TRUE AND target_p.deleted_at IS NULL " +
                "WHERE target_rp.role_id IN (" + placeholders(roleIds.size()) + ") " +
                "AND NOT EXISTS (" +
                " SELECT 1 FROM admin_user_roles own_ur " +
                " JOIN admin_roles own_r ON own_r.id=own_ur.role_id " +
                "  AND own_r.active=TRUE AND own_r.deleted_at IS NULL " +
                " JOIN admin_role_permissions own_rp ON own_rp.role_id=own_r.id " +
                " WHERE own_ur.admin_user_id=? AND own_rp.permission_id=target_rp.permission_id" +
                ")",
            Long.class,
            append(roleIds, operator.getId())
        );
        if (unavailablePermissions != null && unavailablePermissions > 0) {
            throw BusinessException.forbidden("不能分配包含当前账号未拥有权限的角色");
        }
    }

    private void ensureRolePermissionsManageable(AdminUser operator, Long roleId) {
        if (authorization.isSuperAdmin(operator.getId())) return;
        Long unavailable = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT target_rp.permission_id) FROM admin_role_permissions target_rp " +
                "JOIN admin_permissions target_p ON target_p.id=target_rp.permission_id " +
                " AND target_p.active=TRUE AND target_p.deleted_at IS NULL " +
                "WHERE target_rp.role_id=? AND NOT EXISTS (" +
                " SELECT 1 FROM admin_user_roles own_ur " +
                " JOIN admin_roles own_r ON own_r.id=own_ur.role_id " +
                "  AND own_r.active=TRUE AND own_r.deleted_at IS NULL " +
                " JOIN admin_role_permissions own_rp ON own_rp.role_id=own_r.id " +
                " WHERE own_ur.admin_user_id=? AND own_rp.permission_id=target_rp.permission_id" +
                ")",
            Long.class,
            roleId, operator.getId()
        );
        if (unavailable != null && unavailable > 0) {
            throw BusinessException.forbidden("不能管理包含当前账号未拥有权限的角色");
        }
    }

    private void ensureRoleManageable(AdminUser operator, Map<String, Object> role) {
        if (authorization.isSuperAdmin(operator.getId())) return;
        if (((Number) role.get("level")).intValue() <= operatorLevel(operator)) {
            throw BusinessException.forbidden("不能管理同级或更高级别的角色");
        }
    }

    private void ensureRoleLevelManageable(AdminUser operator, int roleLevel) {
        if (!authorization.isSuperAdmin(operator.getId()) && roleLevel <= operatorLevel(operator)) {
            throw BusinessException.forbidden("新角色级别必须低于当前管理员");
        }
    }

    private void ensurePermissionsAssignable(AdminUser operator, List<Long> permissionIds) {
        if (permissionIds.isEmpty() || authorization.isSuperAdmin(operator.getId())) return;
        Long unavailable = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_permissions p WHERE p.id IN (" + placeholders(permissionIds.size()) + ") " +
                "AND p.id NOT IN (SELECT rp.permission_id FROM admin_user_roles ur " +
                "JOIN admin_role_permissions rp ON rp.role_id=ur.role_id WHERE ur.admin_user_id=?)",
            Long.class,
            append(permissionIds, operator.getId())
        );
        if (unavailable != null && unavailable > 0) {
            throw BusinessException.forbidden("不能授予当前账号自身不具备的权限");
        }
    }

    private int operatorLevel(AdminUser operator) {
        Integer level = jdbc.queryForObject(
            "SELECT MIN(r.level_no) FROM admin_user_roles ur JOIN admin_roles r ON r.id=ur.role_id " +
                "WHERE ur.admin_user_id=? AND r.active=TRUE AND r.deleted_at IS NULL",
            Integer.class,
            operator.getId()
        );
        if (level == null) throw BusinessException.forbidden("当前管理员没有可用角色");
        return level;
    }

    private Object[] append(List<Long> ids, Object tail) {
        List<Object> values = new ArrayList<>(ids);
        values.add(tail);
        return values.toArray();
    }

    private String primaryRoleCode(List<Long> roleIds) {
        return jdbc.queryForObject(
            "SELECT code FROM admin_roles WHERE id IN (" + placeholders(roleIds.size()) + ") " +
                "ORDER BY level_no,id LIMIT 1",
            String.class,
            roleIds.toArray()
        );
    }

    private void protectLastSuperAdmin(Long userId, List<Long> newRoleIds, String newStatus) {
        jdbc.queryForObject("SELECT id FROM admin_system_state WHERE id=1 FOR UPDATE", Integer.class);
        boolean currentlySuper = userRoles(userId).stream().anyMatch(role -> "SUPER_ADMIN".equals(role.get("code")));
        if (!currentlySuper) return;
        boolean remainsSuper = "ACTIVE".equals(newStatus) && !newRoleIds.isEmpty() && jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_roles WHERE id IN (" + placeholders(newRoleIds.size()) + ") " +
                "AND code='SUPER_ADMIN' AND active=TRUE AND deleted_at IS NULL",
            Long.class,
            newRoleIds.toArray()
        ) > 0;
        if (remainsSuper) return;
        Long others = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT u.id) FROM admin_users u " +
                "JOIN admin_user_roles ur ON ur.admin_user_id=u.id " +
                "JOIN admin_roles r ON r.id=ur.role_id AND r.code='SUPER_ADMIN' " +
                "WHERE u.id<>? AND u.status='ACTIVE' AND u.deleted_at IS NULL AND r.active=TRUE AND r.deleted_at IS NULL",
            Long.class,
            userId
        );
        if (others == null || others == 0) throw BusinessException.badRequest("平台至少需要保留一个可用的超级管理员");
    }

    private void expireSessionsForRole(Long roleId) {
        jdbc.update(
            "DELETE s FROM admin_sessions s JOIN admin_user_roles ur ON ur.admin_user_id=s.admin_user_id " +
                "WHERE ur.role_id=?",
            roleId
        );
    }

    private void expireSessionsForPermission(Long permissionId) {
        jdbc.update(
            "DELETE s FROM admin_sessions s JOIN admin_user_roles ur ON ur.admin_user_id=s.admin_user_id " +
                "JOIN admin_role_permissions rp ON rp.role_id=ur.role_id WHERE rp.permission_id=?",
            permissionId
        );
    }

    private void refreshPrimaryRolesForRole(Long roleId) {
        List<Long> userIds = jdbc.queryForList(
            "SELECT admin_user_id FROM admin_user_roles WHERE role_id=?",
            Long.class,
            roleId
        );
        for (Long userId : userIds) {
            List<String> roleCodes = jdbc.queryForList(
                "SELECT r.code FROM admin_user_roles ur JOIN admin_roles r ON r.id=ur.role_id " +
                    "WHERE ur.admin_user_id=? AND r.active=TRUE AND r.deleted_at IS NULL " +
                    "ORDER BY r.level_no,r.id LIMIT 1",
                String.class,
                userId
            );
            jdbc.update(
                "UPDATE admin_users SET role=? WHERE id=?",
                roleCodes.isEmpty() ? "NO_ACTIVE_ROLE" : roleCodes.get(0),
                userId
            );
        }
    }

    private AdminUser activeUser(Long id) {
        AdminUser user = users.findById(id).orElseThrow(() -> BusinessException.notFound("后台账号不存在"));
        if (user.getDeletedAt() != null) throw BusinessException.notFound("后台账号不存在");
        return user;
    }

    private Map<String, Object> roleRow(Long id) {
        return jdbc.query(
            "SELECT id,code,name,level_no AS level,description,system_role AS systemRole,active," +
                "created_at AS createdAt FROM admin_roles WHERE id=? AND deleted_at IS NULL",
            result -> {
                if (!result.next()) throw BusinessException.notFound("角色不存在");
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", result.getLong("id"));
                row.put("code", result.getString("code"));
                row.put("name", result.getString("name"));
                row.put("level", result.getInt("level"));
                row.put("description", result.getString("description"));
                row.put("systemRole", result.getBoolean("systemRole"));
                row.put("active", result.getBoolean("active"));
                row.put("createdAt", result.getObject("createdAt"));
                return row;
            },
            id
        );
    }

    private Map<String, Object> permissionRow(Long id) {
        return jdbc.query(
            "SELECT id,code,name,module_name AS moduleName,action_name AS actionName,sort_order AS sortOrder," +
                "system_permission AS systemPermission,active,created_at AS createdAt " +
                "FROM admin_permissions WHERE id=? AND deleted_at IS NULL",
            result -> {
                if (!result.next()) throw BusinessException.notFound("权限不存在");
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", result.getLong("id"));
                row.put("code", result.getString("code"));
                row.put("name", result.getString("name"));
                row.put("moduleName", result.getString("moduleName"));
                row.put("actionName", result.getString("actionName"));
                row.put("sortOrder", result.getInt("sortOrder"));
                row.put("systemPermission", result.getBoolean("systemPermission"));
                row.put("active", result.getBoolean("active"));
                row.put("createdAt", result.getObject("createdAt"));
                return row;
            },
            id
        );
    }

    private void ensurePhoneAvailable(String phone, Long excludedId) {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_users WHERE phone=? AND (? IS NULL OR id<>?)",
            Long.class, phone, excludedId, excludedId
        );
        if (count != null && count > 0) throw BusinessException.badRequest("该手机号已被后台账号使用");
    }

    private void ensureRoleCodeAvailable(String code, Long excludedId) {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_roles WHERE code=? AND (? IS NULL OR id<>?)",
            Long.class, code, excludedId, excludedId
        );
        if (count != null && count > 0) throw BusinessException.badRequest("角色编码已存在");
    }

    private void ensurePermissionCodeAvailable(String code, Long excludedId) {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM admin_permissions WHERE code=? AND (? IS NULL OR id<>?)",
            Long.class, code, excludedId, excludedId
        );
        if (count != null && count > 0) throw BusinessException.badRequest("权限编码已存在");
    }

    private String phone(String value) {
        String result = clean(value);
        if (!result.matches("^1\\d{10}$")) throw BusinessException.badRequest("请输入正确的手机号");
        return result;
    }

    private String status(String value) {
        String result = clean(value).toUpperCase();
        if (result.isEmpty()) return "ACTIVE";
        if (!List.of("ACTIVE", "DISABLED").contains(result)) throw BusinessException.badRequest("后台账号状态无效");
        return result;
    }

    private String validPassword(String value, boolean defaultWhenBlank) {
        String result = clean(value);
        if (result.isEmpty() && defaultWhenBlank) return DEFAULT_RESET_PASSWORD;
        if (result.length() < 10 || result.length() > 128
            || !result.matches(".*[A-Za-z].*") || !result.matches(".*\\d.*")) {
            throw BusinessException.badRequest("密码需为10至128位，并包含字母和数字");
        }
        return result;
    }

    private String roleCode(String value) {
        String result = clean(value).toUpperCase();
        if (!result.matches("^[A-Z][A-Z0-9_]{2,79}$")) throw BusinessException.badRequest("角色编码仅支持大写字母、数字和下划线");
        return result;
    }

    private String permissionCode(String value) {
        String result = clean(value).toUpperCase();
        if (!result.matches("^[A-Z][A-Z0-9_]{2,99}$")) throw BusinessException.badRequest("权限编码仅支持大写字母、数字和下划线");
        return result;
    }

    private int safeLevel(Integer value) {
        int level = value == null ? 100 : value;
        if (level < 0 || level > 9999) throw BusinessException.badRequest("角色级别必须在0至9999之间");
        return level;
    }

    private String required(String value, String message, int maximum) {
        String result = clean(value);
        if (result.isEmpty()) throw BusinessException.badRequest(message);
        if (result.length() > maximum) throw BusinessException.badRequest(message + "，最多" + maximum + "个字");
        return result;
    }

    private String optional(String value, int maximum) {
        String result = clean(value);
        if (result.length() > maximum) throw BusinessException.badRequest("内容最多" + maximum + "个字");
        return result.isEmpty() ? null : result;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private Long number(Object value) {
        return ((Number) value).longValue();
    }

    private String placeholders(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }

    private void audit(AdminUser admin, String action, String type, Object id, String detail, String ip) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUser(admin);
        log.setAction(action);
        log.setTargetType(type);
        log.setTargetId(String.valueOf(id));
        log.setDetail(detail);
        log.setIpAddress(ip);
        audits.save(log);
    }

    public record PageResult(List<Map<String, Object>> items, long total, int page, int size) {
    }
}
