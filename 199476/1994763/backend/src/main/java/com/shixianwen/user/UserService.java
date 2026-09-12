package com.shixianwen.user;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.auth.AuthSessionRepository;
import com.shixianwen.auth.AuthService;
import com.shixianwen.auth.PhoneIdentityHash;
import com.shixianwen.common.BusinessException;
import com.shixianwen.config.AppGlobalSettingService;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.inquiry.InquiryRepository;
import com.shixianwen.wallet.WalletAccount;
import com.shixianwen.wallet.WalletAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StorageVisibility;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class UserService {
    private static final DateTimeFormatter ADJUSTMENT_TIME =
        DateTimeFormatter.ofPattern("M月d日 HH:mm");
    private static final List<String> ACTIVE_INQUIRY_STATUSES =
        List.of("PENDING", "ACTIVE", "TEXT_LIMIT_REACHED", "TEXT_ENDED", "PAID_ACTIVE");

    private final UserRepository userRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final InquiryRepository inquiryRepository;
    private final AuthSessionRepository authSessionRepository;
    private final FileStorage fileStorage;
    private final AnswererEligibilityService answererEligibility;
    private final SensitiveWordService sensitiveWords;
    private final AnalyticsEventService analytics;
    @Autowired(required = false)
    private AppGlobalSettingService globalSettings;

    public UserService(
        UserRepository userRepository,
        WalletAccountRepository walletAccountRepository,
        InquiryRepository inquiryRepository,
        AuthSessionRepository authSessionRepository,
        FileStorage fileStorage,
        AnswererEligibilityService answererEligibility,
        SensitiveWordService sensitiveWords,
        AnalyticsEventService analytics
    ) {
        this.userRepository = userRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.inquiryRepository = inquiryRepository;
        this.authSessionRepository = authSessionRepository;
        this.fileStorage = fileStorage;
        this.answererEligibility = answererEligibility;
        this.sensitiveWords = sensitiveWords;
        this.analytics = analytics;
    }

    @Transactional
    public AuthService.UserView updateAvatar(User user, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty() || avatar.getContentType() == null || !avatar.getContentType().startsWith("image/"))
            throw BusinessException.badRequest("请选择图片文件");
        if (avatar.getSize() > 2L * 1024 * 1024) throw BusinessException.badRequest("头像图片不能超过2MB");
        user.setAvatarUrl(fileStorage.store(
            avatar,
            ("TEST".equals(user.getAccountType()) ? "test/" : "") + "avatars/" + user.getId(),
            StorageVisibility.PUBLIC
        ).publicUrl());
        return AuthService.UserView.from(userRepository.save(user));
    }

    @Transactional
    public AuthService.UserView updateProfile(User user, String nickname, String avatarUrl, String jobTitle) {
        user.setNickname(nickname == null || nickname.isBlank() ? null : sensitiveWords.mask(nickname.trim()));
        user.setJobTitle(jobTitle == null || jobTitle.isBlank() ? null : sensitiveWords.mask(jobTitle.trim()));
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl.isBlank() ? null : avatarUrl.trim());
        }
        return AuthService.UserView.from(userRepository.save(user));
    }

    @Transactional
    public AuthService.UserView setAcceptingInquiries(User user, boolean accepting) {
        User current = userRepository.findById(user.getId())
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        if (accepting && current.getInquiryPriceUpdatedAt() == null) {
            throw BusinessException.badRequest("请先设置每小时费用");
        }
        if (current.isAcceptingInquiries() == accepting) {
            return AuthService.UserView.from(current);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextAdjustment = current.getAcceptingInquiriesUpdatedAt() == null
            ? null
            : current.getAcceptingInquiriesUpdatedAt().plusHours(6);
        if (nextAdjustment != null && nextAdjustment.isAfter(now)) {
            throw BusinessException.badRequest(
                "接受新询问每6小时可切换一次，下次可在" +
                    nextAdjustment.format(ADJUSTMENT_TIME) + "切换"
            );
        }
        if (accepting) {
            answererEligibility.requireQualified(current.getId());
        }
        current.setAcceptingInquiries(accepting);
        current.setAcceptingInquiriesUpdatedAt(now);
        analytics.recordBusinessAfterCommit(current, "answerer_setting_saved", java.util.Map.of(
            "accepting_inquiries", accepting,
            "hourly_rate", current.getInquiryHourlyRate()
        ));
        return AuthService.UserView.from(userRepository.save(current));
    }

    @Transactional
    public AuthService.UserView setInquiryHourlyRate(User user, int hourlyRate) {
        AppGlobalSettingService.Settings settings = globalSettings == null ? null : globalSettings.current();
        int minimum = settings == null ? 1 : settings.hourlyRateMin();
        int maximum = settings == null ? 5000 : settings.hourlyRateMax();
        if (hourlyRate < minimum || hourlyRate > maximum) {
            throw BusinessException.badRequest("每小时费用须在" + minimum + "至" + maximum + "元之间");
        }
        User current = userRepository.findById(user.getId())
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        if (current.getInquiryPriceUpdatedAt() != null && current.getInquiryHourlyRate() == hourlyRate) {
            return AuthService.UserView.from(current);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextAdjustment = current.getInquiryPriceUpdatedAt() == null
            ? null
            : current.getInquiryPriceUpdatedAt().plusMonths(3);
        if (nextAdjustment != null && nextAdjustment.isAfter(now)) {
            throw BusinessException.badRequest(
                "每小时费用每3个月可调整一次，下次可在" +
                    nextAdjustment.format(ADJUSTMENT_TIME) + "调整"
            );
        }
        current.setInquiryHourlyRate(hourlyRate);
        current.setInquiryPriceUpdatedAt(now);
        analytics.recordBusinessAfterCommit(current, "answerer_setting_saved", java.util.Map.of(
            "accepting_inquiries", current.isAcceptingInquiries(),
            "hourly_rate", hourlyRate
        ));
        return AuthService.UserView.from(userRepository.save(current));
    }

    @Transactional
    public AuthService.UserView dismissPlatformIntroduction(User user) {
        User current = userRepository.findById(user.getId())
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        current.setPlatformIntroRequired(false);
        return AuthService.UserView.from(userRepository.save(current));
    }

    @Transactional(readOnly = true)
    public AccountDeletionEligibility accountDeletionEligibility(User user) {
        WalletAccount wallet = walletAccountRepository.findByUserId(user.getId())
            .orElseThrow(() -> BusinessException.notFound("账户余额不存在"));
        boolean availableBalanceCleared = wallet.getAvailableBalance().compareTo(BigDecimal.ZERO) == 0;
        boolean frozenBalanceCleared = wallet.getFrozenBalance().compareTo(BigDecimal.ZERO) == 0;
        boolean noActiveInquiries = !hasActiveInquiry(user.getId());
        return new AccountDeletionEligibility(
            availableBalanceCleared && frozenBalanceCleared && noActiveInquiries,
            availableBalanceCleared,
            frozenBalanceCleared,
            noActiveInquiries,
            wallet.getAvailableBalance(),
            wallet.getFrozenBalance()
        );
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
        if (hasActiveInquiry(user.getId())) {
            throw BusinessException.badRequest("仍有未结束询问，暂时不能注销");
        }

        authSessionRepository.deleteByUserId(user.getId());
        user.setAccountStatus("DELETED");
        user.setAcceptingInquiries(false);
        user.setAnswererStatus("CLOSED");
        user.setNickname(null);
        user.setAvatarUrl(null);
        user.setDeletedPhoneHash(PhoneIdentityHash.of(user.getPhone()));
        user.setPhone(deletedPhone(user.getId()));
        userRepository.save(user);
        analytics.recordBusinessAfterCommit(user, "account_deleted", java.util.Map.of());
    }

    private static String deletedPhone(Long userId) {
        return "d" + userId;
    }

    private boolean hasActiveInquiry(Long userId) {
        return inquiryRepository.existsByQuestionerIdAndStatusIn(userId, ACTIVE_INQUIRY_STATUSES)
            || inquiryRepository.existsByAnswererIdAndStatusIn(userId, ACTIVE_INQUIRY_STATUSES);
    }

    public record AccountDeletionEligibility(
        boolean eligible,
        boolean availableBalanceCleared,
        boolean frozenBalanceCleared,
        boolean noActiveInquiries,
        BigDecimal availableBalance,
        BigDecimal frozenBalance
    ) {
    }
}
