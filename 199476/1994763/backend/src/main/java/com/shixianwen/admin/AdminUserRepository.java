package com.shixianwen.admin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByPhoneAndStatusAndDeletedAtIsNull(String phone, String status);
}
