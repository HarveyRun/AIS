package com.shixianwen.user;

import com.shixianwen.auth.AuthSessionRepository;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.inquiry.InquiryRepository;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.wallet.WalletAccountRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
