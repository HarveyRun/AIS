package com.shixianwen.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AppTestLoginAccountRepository extends JpaRepository<AppTestLoginAccount, Long> {
    Optional<AppTestLoginAccount> findByPhoneAndEnabledTrueAndDeletedFalse(String phone);

    Optional<AppTestLoginAccount> findByPhone(String phone);

    Optional<AppTestLoginAccount> findByIdAndDeletedFalse(Long id);

    Page<AppTestLoginAccount> findAllByDeletedFalse(Pageable pageable);
}
