package com.shixianwen.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformFeeSettingRepository extends JpaRepository<PlatformFeeSetting, Long> {
    Optional<PlatformFeeSetting> findByClientPlatform(String clientPlatform);
}
