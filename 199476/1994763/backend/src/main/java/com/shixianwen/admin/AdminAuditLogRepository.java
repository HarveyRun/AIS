package com.shixianwen.admin;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {}
