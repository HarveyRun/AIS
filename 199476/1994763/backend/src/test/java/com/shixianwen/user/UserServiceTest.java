package com.shixianwen.user;

import com.shixianwen.auth.AuthSessionRepository;
import com.shixianwen.common.BusinessException;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.inquiry.InquiryRepository;
import com.shixianwen.storage.FileStorage;
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
            sensitiveWords
        );
        User user = new User();
        user.setAvatarUrl("https://cdn.example.com/avatar.jpg");
        when(sensitiveWords.mask("新昵称")).thenReturn("新昵称");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateProfile(user, "新昵称", null);

        assertEquals("新昵称", user.getNickname());
        assertEquals("https://cdn.example.com/avatar.jpg", user.getAvatarUrl());
    }

    @Test
    void inquiryPriceRangeMustBeOrderedAndIsPersisted() {
        UserRepository users = mock(UserRepository.class);
        UserService service = new UserService(
            users,
            mock(WalletAccountRepository.class),
            mock(InquiryRepository.class),
            mock(AuthSessionRepository.class),
            mock(FileStorage.class),
            mock(AnswererEligibilityService.class),
            mock(SensitiveWordService.class)
        );
        User user = new User();
        user.setId(1L);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(
            BusinessException.class,
            () -> service.setInquiryPriceRange(user, 500, 100)
        );
        service.setInquiryPriceRange(user, 50, 300);

        assertEquals(50, user.getInquiryPriceMin());
        assertEquals(300, user.getInquiryPriceMax());
        assertThrows(
            BusinessException.class,
            () -> service.setInquiryPriceRange(user, 60, 300)
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
            mock(SensitiveWordService.class)
        );
        User user = new User();
        user.setId(1L);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(
            BusinessException.class,
            () -> service.setAcceptingInquiries(user, true)
        );
        service.setInquiryPriceRange(user, 1, 5000);
        service.setAcceptingInquiries(user, true);
        assertThrows(
            BusinessException.class,
            () -> service.setAcceptingInquiries(user, false)
        );
    }
}
