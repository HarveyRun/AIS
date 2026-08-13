package com.shixianwen.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByPhoneAndAccountStatus(String phone, String accountStatus);
    Optional<User> findByUidAndAccountStatus(String uid, String accountStatus);
}
