package com.shixianwen.admin;

import com.shixianwen.common.BusinessException;
import com.shixianwen.security.LoginAttemptService;
import com.shixianwen.security.SecurityEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAuthServiceTest {
    private AdminUserRepository users;
    private AdminSessionRepository sessions;
    private AdminPasswordEncoder passwords;
    private LoginAttemptService attempts;
    private AdminAuthorizationService authorization;
    private AdminAuthService service;

    @BeforeEach
    void setUp() {
        users = mock(AdminUserRepository.class);
        sessions = mock(AdminSessionRepository.class);
        passwords = mock(AdminPasswordEncoder.class);
        attempts = mock(LoginAttemptService.class);
        authorization = mock(AdminAuthorizationService.class);
        service = new AdminAuthService(
            users, sessions, passwords, mock(JdbcTemplate.class), attempts,
            mock(SecurityEventService.class), authorization
        );
    }

    @Test
    void missingAccountStillRunsPasswordDerivationAndRecordsFailure() {
        when(users.findByPhoneAndStatusAndDeletedAtIsNull("18800000000", "ACTIVE"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(
            "18800000000", "wrong-password", "127.0.0.1", "admin-device-001"
        )).isInstanceOf(BusinessException.class);

        verify(passwords).matches("wrong-password", null);
        verify(attempts).record("ADMIN", "18800000000", "127.0.0.1", "admin-device-001", false);
    }

    @Test
    void validPasswordWithoutActiveRoleDoesNotCreateSession() {
        AdminUser user = new AdminUser();
        user.setId(9L);
        user.setPhone("18800000000");
        user.setPasswordHash("hash");
        when(users.findByPhoneAndStatusAndDeletedAtIsNull("18800000000", "ACTIVE"))
            .thenReturn(Optional.of(user));
        when(passwords.matches("correct-password", "hash")).thenReturn(true);
        when(authorization.roles(9L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.login(
            "18800000000", "correct-password", "127.0.0.1", "admin-device-001"
        )).isInstanceOf(BusinessException.class).hasMessageContaining("没有可用角色");

        verify(sessions, never()).save(org.mockito.ArgumentMatchers.any());
        verify(attempts, never()).record(
            "ADMIN", "18800000000", "127.0.0.1", "admin-device-001", true
        );
    }
}
