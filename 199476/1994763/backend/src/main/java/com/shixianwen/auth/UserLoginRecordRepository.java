package com.shixianwen.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLoginRecordRepository extends JpaRepository<UserLoginRecord, Long> {
}
