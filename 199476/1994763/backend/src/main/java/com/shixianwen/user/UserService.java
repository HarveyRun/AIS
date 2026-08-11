package com.shixianwen.user;

import com.shixianwen.auth.AuthSessionRepository;
import com.shixianwen.auth.AuthService;
import com.shixianwen.common.BusinessException;
import com.shixianwen.inquiry.InquiryRepository;
import com.shixianwen.wallet.WalletAccount;
import com.shixianwen.wallet.WalletAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import com.shixianwen.storage.FileStorage;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {
    private static final List<String> ACTIVE_INQUIRY_STATUSES =
        List.of("PENDING", "ACTIVE", "AWAITING_CONFIRMATION", "DISPUTED");

    private final UserRepository userRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final InquiryRepository inquiryRepository;
    private final AuthSessionRepository authSessionRepository;
    private final FileStorage fileStorage;

    public UserService(
        UserRepository userRepository,
        WalletAccountRepository walletAccountRepository,
        InquiryRepository inquiryRepository,
        AuthSessionRepository authSessionRepository,
        FileStorage fileStorage
    ) {
        this.userRepository = userRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.inquiryRepository = inquiryRepository;
        this.authSessionRepository = authSessionRepository;
        this.fileStorage = fileStorage;
    }

    @Transactional
    public AuthService.UserView updateAvatar(User user, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty() || avatar.getContentType() == null || !avatar.getContentType().startsWith("image/"))
            throw BusinessException.badRequest("请选择图片文件");
        if (avatar.getSize() > 2L * 1024 * 1024) throw BusinessException.badRequest("头像图片不能超过2MB");
        user.setAvatarUrl(fileStorage.store(avatar, "avatars/" + user.getId()).publicUrl());
        return AuthService.UserView.from(userRepository.save(user));
    }

    @Transactional
    public AuthService.UserView updateProfile(User user, String nickname, String avatarUrl) {
        user.setNickname(nickname == null || nickname.isBlank() ? null : nickname.trim());
        user.setAvatarUrl(avatarUrl == null || avatarUrl.isBlank() ? null : avatarUrl.trim());
        return AuthService.UserView.from(userRepository.save(user));
    }

    @Transactional
    public AuthService.UserView setAcceptingInquiries(User user, boolean accepting) {
        if (!"APPROVED".equals(user.getAnswererStatus())) {
            throw BusinessException.forbidden("完成基础信息认证后才能接受询问");
        }
        user.setAcceptingInquiries(accepting);
        return AuthService.UserView.from(userRepository.save(user));
    }

    @Transactional
    public void deleteAccount(User user) {
        WalletAccount wallet = walletAccountRepository.findWithLockByUserId(user.getId())
            .orElseThrow(() -> BusinessException.notFound("账户余额不存在"));
        if (wallet.getAvailableBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw BusinessException.badRequest("请先处理账户可用余额");
        }
        if (wallet.getFrozenBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw BusinessException.badRequest("仍有冻结金额，暂时不能注销");
        }
        boolean hasActiveInquiry = inquiryRepository.existsByQuestionerIdAndStatusIn(user.getId(), ACTIVE_INQUIRY_STATUSES)
            || inquiryRepository.existsByAnswererIdAndStatusIn(user.getId(), ACTIVE_INQUIRY_STATUSES);
        if (hasActiveInquiry) {
            throw BusinessException.badRequest("仍有未结束询问，暂时不能注销");
        }

        authSessionRepository.deleteByUserId(user.getId());
        user.setAccountStatus("DELETED");
        user.setAcceptingInquiries(false);
        user.setAnswererStatus("CLOSED");
        user.setNickname(null);
        user.setAvatarUrl(null);
        user.setPhone("deleted-" + user.getId() + "-" + System.currentTimeMillis());
        userRepository.save(user);
    }
}
