package com.shixianwen.user;

import com.shixianwen.auth.AuthSessionRepository;
import com.shixianwen.auth.PhoneIdentityHash;
import com.shixianwen.common.BusinessException;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.inquiry.InquiryRepository;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.wallet.WalletAccount;
import com.shixianwen.wallet.WalletAccountRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {
    @Test
    void updatingNicknameDoesNotClearExistingAvatarWhenAvatarIsOmitted() {
        UserRepository users = mock(UserRepository.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        UserService service = new UserService(
            users,
            mock(WalletAccountRepository.class),
            mock(InquiryRepository.class),
            mock(AuthSessionRepository.class),
            mock(FileStorage.class),
            mock(AnswererEligibilityService.class),
            sensitiveWords,
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setAvatarUrl("https://cdn.example.com/avatar.jpg");
        when(sensitiveWords.mask("新昵称")).thenReturn("新昵称");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateProfile(user, "新昵称", null, "产品经理");

        assertEquals("新昵称", user.getNickname());
        assertEquals("https://cdn.example.com/avatar.jpg", user.getAvatarUrl());
    }

    @Test
    void inquiryHourlyRateMustBeWithinRangeAndIsPersisted() {
        UserRepository users = mock(UserRepository.class);
        UserService service = new UserService(
            users,
            mock(WalletAccountRepository.class),
            mock(InquiryRepository.class),
            mock(AuthSessionRepository.class),
            mock(FileStorage.class),
            mock(AnswererEligibilityService.class),
            mock(SensitiveWordService.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setId(1L);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(
            BusinessException.class,
            () -> service.setInquiryHourlyRate(user, 0)
        );
        service.setInquiryHourlyRate(user, 300);

        assertEquals(300, user.getInquiryHourlyRate());
        assertThrows(
            BusinessException.class,
            () -> service.setInquiryHourlyRate(user, 5001)
        );
    }

    @Test
    void acceptingRequiresPriceSetupAndCanOnlySwitchEverySixHours() {
        UserRepository users = mock(UserRepository.class);
        UserService service = new UserService(
            users,
            mock(WalletAccountRepository.class),
            mock(InquiryRepository.class),
            mock(AuthSessionRepository.class),
            mock(FileStorage.class),
            mock(AnswererEligibilityService.class),
            mock(SensitiveWordService.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setId(1L);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(
            BusinessException.class,
            () -> service.setAcceptingInquiries(user, true)
        );
        service.setInquiryHourlyRate(user, 100);
        service.setAcceptingInquiries(user, true);
        assertThrows(
            BusinessException.class,
            () -> service.setAcceptingInquiries(user, false)
        );
    }

    @Test
    void deletingAccountUsesUniquePhoneMarkerWithinDatabaseColumnLength() {
        UserRepository users = mock(UserRepository.class);
        WalletAccountRepository wallets = mock(WalletAccountRepository.class);
        InquiryRepository inquiries = mock(InquiryRepository.class);
        AuthSessionRepository sessions = mock(AuthSessionRepository.class);
        UserService service = new UserService(
            users,
            wallets,
            inquiries,
            sessions,
            mock(FileStorage.class),
            mock(AnswererEligibilityService.class),
            mock(SensitiveWordService.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setId(Long.MAX_VALUE);
        user.setPhone("15611111111");
        WalletAccount wallet = new WalletAccount();
        when(wallets.findWithLockByUserId(Long.MAX_VALUE)).thenReturn(Optional.of(wallet));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteAccount(user);

        assertEquals("DELETED", user.getAccountStatus());
        assertEquals("d9223372036854775807", user.getPhone());
        assertEquals(20, user.getPhone().length());
        assertEquals(PhoneIdentityHash.of("15611111111"), user.getDeletedPhoneHash());
    }
}
