package com.shixianwen.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SecurityEventRepository
    extends JpaRepository<SecurityEvent, Long>, JpaSpecificationExecutor<SecurityEvent> {
}
