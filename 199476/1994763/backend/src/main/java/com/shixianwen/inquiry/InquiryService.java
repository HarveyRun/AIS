package com.shixianwen.inquiry;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.common.BusinessException;
import com.shixianwen.config.AppGlobalSettingService;
import com.shixianwen.content.SensitiveContentCipher;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.user.AnswererEligibilityService;
import com.shixianwen.wallet.WalletService;
import com.shixianwen.wallet.MoneyAmounts;
import com.shixianwen.network.ClientNetworkInfo;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StoredFile;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.security.SecurityEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InquiryService {
    private static final Set<String> OPEN = Set.of(
        "PENDING", "ACTIVE", "TEXT_LIMIT_REACHED"
    );
    private static final Set<String> CAPACITY_OCCUPYING = Set.of(
        "ACTIVE", "TEXT_LIMIT_REACHED"
    );
    private static final int CURRENT_FLOW_VERSION = 2;
    private static final int INITIAL_TEXT_MESSAGE_LIMIT = 50;
    private static final Set<String> FREE_MESSAGE_TYPES = Set.of("TEXT", "IMAGE");
    private static final BigDecimal INQUIRY_DEPOSIT = new BigDecimal("2.00");
    private final InquiryRepository inquiries;
    private final InquiryMessageRepository messages;
    private final UserRepository users;
    private final WalletService wallet;
    private final NotificationService notifications;
    private final RealtimePublisher realtime;
    private final FileStorage fileStorage;
    private final AnswererEligibilityService answererEligibility;
    private final SensitiveWordService sensitiveWords;
    private final SensitiveContentCipher sensitiveContentCipher;
    private final ChatAbuseGuard chatAbuseGuard;
    private final SecurityEventService securityEvents;
    private final AnalyticsEventService analytics;
    private final JdbcTemplate jdbc;
    private final UserCommunicationBlockService communicationBlocks;

    @Autowired
    private InquiryVoiceCallRepository voiceCalls;

    @Autowired(required = false)
    private AppGlobalSettingService globalSettings;

    @Transactional
    public InquiryView create(Long questionerId, CreateCommand command, ClientNetworkInfo network) {
        AppGlobalSettingService.Settings settings = settings();
        if (settings != null && !settings.inquiryEnabled()) {
            throw BusinessException.badRequest("平台暂时关闭了发起询问");
        }
        if (questionerId.equals(command.answererId())) throw BusinessException.badRequest("不能向自己发起询问");
        LockedParticipants participants = lockParticipants(questionerId, command.answererId());
        User questioner = participants.questioner();
        User answerer = participants.answerer();
        requireCommunicationAllowed(questionerId, answerer.getId());
        if (!questioner.getAccountType().equals(answerer.getAccountType())) {
            securityEvents.recordSafely(
                questionerId, null, "CROSS_ENVIRONMENT_INQUIRY_BLOCKED", "HIGH",
                network.ipAddress(), null, "answererId=" + answerer.getId()
            );
            throw BusinessException.forbidden("测试账号与普通账号不能互相发起询问");
        }
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        if (inquiries.countByQuestionerIdAndCreatedAtAfter(questionerId, startOfDay) >= 20) {
            securityEvents.recordSafely(
                questionerId, null, "INQUIRY_DAILY_LIMIT", "HIGH", network.ipAddress(), null, null
            );
            throw BusinessException.tooManyRequests("今日发起询问次数已达上限");
        }
        if (inquiries.countByQuestionerIdAndAnswererIdAndCreatedAtAfter(
            questionerId, answerer.getId(), startOfDay
        ) >= 3) {
            securityEvents.recordSafely(
                questionerId, null, "INQUIRY_COUNTERPART_LIMIT", "HIGH", network.ipAddress(), null,
                "answererId=" + answerer.getId()
            );
            throw BusinessException.tooManyRequests("今日向同一人发起询问次数已达上限");
        }
        if (network.ipAddress() != null && network.ipAddress().equals(answerer.getLastLoginIp())) {
            securityEvents.recordSafely(
                questionerId, null, "INQUIRY_SHARED_IP", "HIGH", network.ipAddress(), null,
                "answererId=" + answerer.getId()
            );
        }
        answererEligibility.requireAvailable(answerer.getId());
        InquiryCapacity capacity = inquiryCapacity();
        requireInquiryCapacity(questionerId, answerer.getId(), capacity);
        if (inquiries.existsByQuestionerIdAndAnswererIdAndStatusIn(questionerId, answerer.getId(), OPEN))
            throw BusinessException.badRequest("你们已有一条进行中的询问");
        int freePendingLimit = settings == null ? 3 : settings.freePendingInquiryLimit();
        BigDecimal depositAmount = settings == null ? INQUIRY_DEPOSIT : settings.inquiryDepositAmount();
        boolean depositRequired = inquiries.countByQuestionerIdAndStatus(questionerId, "PENDING") >= freePendingLimit;
        List<InquiryExperience> matchedExperiences = jdbc.query(
            "SELECT title FROM certifications WHERE id=? AND user_id=? " +
                "AND category='EXPERIENCE' " +
                "AND status='APPROVED' AND enabled=TRUE AND deleted_at IS NULL",
            (resultSet, rowNumber) -> new InquiryExperience(resultSet.getString("title")),
            command.sourceExperienceCertificationId(),
            answerer.getId()
        );
        if (matchedExperiences.isEmpty()) {
            throw BusinessException.badRequest("这段亲身经历已不可询问");
        }
        InquiryExperience selectedExperience = matchedExperiences.get(0);
        String topic = selectedExperience.title();
        String question = required(command.question(), "请填写你的情况", 200);
        Inquiry item = new Inquiry();
        item.setQuestioner(questioner); item.setAnswerer(answerer); item.setTopic(sensitiveWords.mask(topic));
        item.setSourceType("EXPERIENCE");
        item.setSourceExperienceCertificationId(command.sourceExperienceCertificationId());
        item.setQuestion(sensitiveWords.mask(question));
        item.setQuestionRawEncrypted(sensitiveContentCipher.encrypt(question));
        item.setRequestIp(network.ipAddress());
        item.setRequestLocation(network.location());
        item.setAmount(MoneyAmounts.ZERO);
        item.setDepositAmount(depositRequired ? depositAmount : MoneyAmounts.ZERO);
        item.setDepositStatus(depositRequired ? "FROZEN" : "NONE");
        item.setSettleableAmount(MoneyAmounts.ZERO);
        item.setClientPlatform(normalizePlatform(command.clientPlatform()));
        item.setFlowVersion(CURRENT_FLOW_VERSION);
        item.setHourlyRateSnapshot(answerer.getInquiryHourlyRate());
        int initialTextLimit = settings == null ? INITIAL_TEXT_MESSAGE_LIMIT : settings.initialTextMessageLimit();
        item.setQuestionerTextLimit(initialTextLimit);
        item.setAnswererTextLimit(initialTextLimit);
        item.setStatus("PENDING"); item.setFundsStatus("NONE");
        item.setAnswererUnreadCount(1);
        int responseHours = settings == null ? 72 : settings.inquiryResponseTimeoutHours();
        item.setResponseDeadline(LocalDateTime.now().plusHours(responseHours));
        item = inquiries.save(item);
        if (depositRequired) {
            WalletService.FrozenAllocation deposit = wallet.freezeInquiryDeposit(
                questionerId,
                item.getDepositAmount(),
                item.getId()
            );
            item.setDepositFrozenRechargeAmount(deposit.rechargeAmount());
            item.setDepositFrozenIncomeAmount(deposit.incomeAmount());
        }
        notifications.send(answerer, "收到新的询问", displayName(questioner) + "：" + notificationSubject(item), "/inquiries/" + item.getId());
        publishInquiryChanged(answerer.getId(), item);
        analytics.recordBusinessAfterCommit(questioner, "inquiry_created", java.util.Map.of(
            "inquiry_id", item.getId(),
            "answerer_id", answerer.getId(),
            "source_type", item.getSourceType(),
            "platform", item.getClientPlatform(),
            "flow_version", item.getFlowVersion()
        ));
        return view(item, questionerId);
    }

    public record InquiryExperience(String title) {}

    @Transactional(readOnly = true)
    public List<InquiryView> list(Long userId) {
        Map<Long, LocalDateTime> lastSentAt = messages.findLastSentAtBySenderId(userId).stream()
            .collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> (LocalDateTime) row[1]
            ));
        List<Inquiry> items = new ArrayList<>(
            inquiries.findByQuestionerIdOrAnswererIdOrderByCreatedAtDesc(userId, userId)
        );
        items.sort(
            Comparator.comparing(
                (Inquiry item) -> lastSentAt.getOrDefault(item.getId(), item.getCreatedAt()),
                Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed().thenComparing(Inquiry::getId, Comparator.reverseOrder())
        );
        return items.stream().map(item -> view(item, userId)).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        Long count = jdbc.queryForObject(
            "SELECT COALESCE(SUM(CASE " +
                "WHEN questioner_id=? THEN questioner_unread_count " +
                "WHEN answerer_id=? THEN answerer_unread_count ELSE 0 END),0) " +
                "FROM inquiries WHERE questioner_id=? OR answerer_id=?",
            Long.class,
            userId,
            userId,
            userId,
            userId
        );
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    public InquiryDetail detail(Long userId, Long inquiryId) {
        Inquiry item = accessible(userId, inquiryId);
        return new InquiryDetail(view(item, userId), messages.findByInquiryIdOrderByCreatedAtAsc(inquiryId).stream()
                .map(this::messageView).toList());
    }

    @Transactional
    public void read(Long userId, Long inquiryId) {
        Inquiry item = lockedAccessible(userId, inquiryId);
        boolean changed;
        if (item.getQuestioner().getId().equals(userId)) {
            changed = item.getQuestionerUnreadCount() > 0;
            item.setQuestionerUnreadCount(0);
        } else {
            changed = item.getAnswererUnreadCount() > 0;
            item.setAnswererUnreadCount(0);
        }
        if (changed) {
            realtime.afterCommit(userId, "INQUIRY_READ", java.util.Map.of("inquiryId", inquiryId));
        }
    }

    @Transactional
    public InquiryView accept(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, true);
        requireCommunicationAllowed(item);
        requireStatus(item, "PENDING");
        lockParticipants(item);
        InquiryCapacity capacity = inquiryCapacity();
        requireInquiryCapacity(item.getQuestioner().getId(), userId, capacity);
        answererEligibility.requireCanAccept(userId);
        item.setAnswererUnreadCount(0);
        LocalDateTime acceptedAt = LocalDateTime.now();
        item.setStatus("ACTIVE"); item.setAcceptedAt(acceptedAt); item.setResponseDeadline(null);
        int maxDays = settings() == null ? 20 : settings().inquiryMaxDurationDays();
        item.setConversationExpiresAt(acceptedAt.plusDays(maxDays));
        increaseUnread(item, item.getQuestioner().getId());
        notifications.send(item.getQuestioner(), "询问已接受", displayName(item.getAnswerer()) + "已接受：" + notificationSubject(item), "/inquiries/" + item.getId());
        publishInquiryChanged(item.getQuestioner().getId(), item);
        analytics.recordBusinessAfterCommit(item.getAnswerer(), "inquiry_accepted", java.util.Map.of(
            "inquiry_id", item.getId(),
            "platform", item.getClientPlatform(),
            "response_minutes", java.time.Duration.between(item.getCreatedAt(), item.getAcceptedAt()).toMinutes()
        ));
        return view(item, userId);
    }

    @Transactional
    public InquiryView reject(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, true);
        requireStatus(item, "PENDING");
        item.setAnswererUnreadCount(0);
        closePending(item, "REJECTED");
        increaseUnread(item, item.getQuestioner().getId());
        notifications.send(item.getQuestioner(), "询问未被接受", notificationSubject(item), "/inquiries/" + item.getId());
        publishInquiryChanged(item.getQuestioner().getId(), item);
        analytics.recordBusinessAfterCommit(item.getAnswerer(), "inquiry_rejected", java.util.Map.of(
            "inquiry_id", item.getId(), "platform", item.getClientPlatform()
        ));
        return view(item, userId);
    }

    @Transactional
    public InquiryView cancel(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, false);
        requireStatus(item, "PENDING");
        item.setQuestionerUnreadCount(0);
        closePending(item, "CANCELLED");
        increaseUnread(item, item.getAnswerer().getId());
        notifications.send(item.getAnswerer(), "询问已撤销", displayName(item.getQuestioner()) + "撤销了：" + notificationSubject(item), "/inquiries/" + item.getId());
        publishInquiryChanged(item.getAnswerer().getId(), item);
        analytics.recordBusinessAfterCommit(item.getQuestioner(), "inquiry_cancelled", java.util.Map.of(
            "inquiry_id", item.getId(), "platform", item.getClientPlatform()
        ));
        return view(item, userId);
    }

    @Transactional
    public MessageView send(Long userId, Long inquiryId, String content) {
        Inquiry item = lockedAccessible(userId, inquiryId);
        requireCommunicationAllowed(item);
        requireMessagingStatus(item, userId);
        if (item.getFlowVersion() < CURRENT_FLOW_VERSION) {
            requireConsecutiveMessageCapacity(item, userId);
        } else {
            requireTextMessageCapacity(item, userId);
        }
        chatAbuseGuard.requireTextAllowed(inquiryId, userId, content == null ? "" : content.trim());
        if (item.getQuestioner().getId().equals(userId)) item.setQuestionerUnreadCount(0);
        else item.setAnswererUnreadCount(0);
        InquiryMessage message = new InquiryMessage();
        message.setInquiry(item); message.setSender(user(userId));
        message.setCountsTowardFreeLimit(true);
        int messageLimit = item.getFlowVersion() >= CURRENT_FLOW_VERSION
            ? settings() == null ? 100 : settings().textMessageMaxLength()
            : 500;
        String originalContent = required(content, "消息不能为空", messageLimit);
        message.setRawContentEncrypted(sensitiveContentCipher.encrypt(originalContent));
        message.setContent(sensitiveWords.mask(originalContent));
        message = messages.saveAndFlush(message);
        item.setLastMessageAt(message.getCreatedAt());
        if (item.getFlowVersion() < CURRENT_FLOW_VERSION) {
            processOverdueBeforeReply(item, userId, message.getCreatedAt());
        }
        if (item.getFlowVersion() < CURRENT_FLOW_VERSION && "ACTIVE".equals(item.getStatus())) {
            updateReplyCycle(item, userId, message.getCreatedAt());
        } else if (item.getFlowVersion() >= CURRENT_FLOW_VERSION) {
            updateFirstMessageTimes(item, userId, message.getCreatedAt());
            closeWhenTextLimitReached(item, userId);
        }
        Long recipientId = item.getQuestioner().getId().equals(userId)
            ? item.getAnswerer().getId()
            : item.getQuestioner().getId();
        increaseUnread(item, recipientId);
        MessageView view = messageView(message);
        realtime.afterCommit(recipientId, "INQUIRY_MESSAGE", new MessageEvent(
            inquiryId, unreadFor(item, recipientId), view
        ));
        analytics.recordBusinessAfterCommit(message.getSender(), "message_sent", java.util.Map.of(
            "inquiry_id", inquiryId,
            "message_type", "TEXT",
            "sender_role", item.getQuestioner().getId().equals(userId) ? "QUESTIONER" : "ANSWERER",
            "platform", item.getClientPlatform()
        ));
        return view;
    }

    @Transactional
    public MessageView sendImage(Long userId, Long inquiryId, MultipartFile image) {
        Inquiry item = lockedAccessible(userId, inquiryId);
        requireCommunicationAllowed(item);
        if (!Set.of("ACTIVE", "TEXT_LIMIT_REACHED").contains(item.getStatus())) {
            throw BusinessException.badRequest("当前状态不能发送图片");
        }
        if (!voiceCalls.existsByInquiryIdAndStatusIn(inquiryId, Set.of("ACTIVE"))) {
            throw BusinessException.badRequest("语音通话中才能发送图片");
        }
        chatAbuseGuard.requireImageAllowed(inquiryId, userId);
        validateChatImage(image);
        if (item.getQuestioner().getId().equals(userId)) item.setQuestionerUnreadCount(0);
        else item.setAnswererUnreadCount(0);

        StoredFile stored = fileStorage.store(
            image,
            storagePrefix(user(userId)) + "inquiries/" + inquiryId,
            StorageVisibility.PRIVATE
        );
        InquiryMessage message = new InquiryMessage();
        message.setInquiry(item);
        message.setSender(user(userId));
        message.setMessageType("IMAGE");
        message.setCountsTowardFreeLimit(false);
        message.setContent("");
        message.setAttachmentKey(stored.storageKey());
        message.setAttachmentUrl(null);
        message.setAttachmentName(clean(image.getOriginalFilename(), 255));
        message.setAttachmentSize(stored.size());
        message = messages.saveAndFlush(message);
        item.setLastMessageAt(message.getCreatedAt());
        if (item.getFlowVersion() < CURRENT_FLOW_VERSION) {
            processOverdueBeforeReply(item, userId, message.getCreatedAt());
        }
        if (item.getFlowVersion() < CURRENT_FLOW_VERSION && "ACTIVE".equals(item.getStatus())) {
            updateReplyCycle(item, userId, message.getCreatedAt());
        } else if (item.getFlowVersion() >= CURRENT_FLOW_VERSION) {
            updateFirstMessageTimes(item, userId, message.getCreatedAt());
            closeWhenTextLimitReached(item, userId);
        }

        Long recipientId = item.getQuestioner().getId().equals(userId)
            ? item.getAnswerer().getId()
            : item.getQuestioner().getId();
        increaseUnread(item, recipientId);
        MessageView view = messageView(message);
        realtime.afterCommit(recipientId, "INQUIRY_MESSAGE", new MessageEvent(
            inquiryId, unreadFor(item, recipientId), view
        ));
        analytics.recordBusinessAfterCommit(message.getSender(), "message_sent", java.util.Map.of(
            "inquiry_id", inquiryId,
            "message_type", "IMAGE",
            "sender_role", item.getQuestioner().getId().equals(userId) ? "QUESTIONER" : "ANSWERER",
            "platform", item.getClientPlatform()
        ));
        return view;
    }

    @Transactional
    public InquiryView endInquiry(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, false);
        if (item.getFlowVersion() < CURRENT_FLOW_VERSION) {
            throw BusinessException.badRequest("该询问不能直接结束");
        }
        if (!Set.of("ACTIVE", "TEXT_LIMIT_REACHED").contains(item.getStatus())) {
            throw BusinessException.badRequest("当前状态不能结束交流");
        }
        Long openVoiceCallCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM inquiry_voice_calls " +
                "WHERE inquiry_id=? AND status IN ('CONNECTING','ACTIVE')",
            Long.class,
            inquiryId
        );
        if (openVoiceCallCount != null && openVoiceCallCount > 0) {
            throw BusinessException.badRequest("请先结束当前语音通话");
        }
        closeWithoutFunds(item, "COMPLETED", "提问者已结束本次询问。");
        Long otherId = item.getAnswerer().getId();
        increaseUnread(item, otherId);
        notifications.send(user(otherId), "本次询问已结束", notificationSubject(item), "/inquiries/" + item.getId());
        publishInquiryChanged(otherId, item);
        return view(item, userId);
    }

    @Scheduled(fixedDelayString = "${app.inquiry.timeout-scan-ms:60000}")
    @Transactional
    public void processTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        jdbc.queryForList(
            "SELECT id FROM inquiries WHERE status='PENDING' AND response_deadline<=?",
            Long.class,
            now
        ).forEach(id -> {
            Inquiry item = inquiries.findWithLockById(id).orElse(null);
            if (item != null && "PENDING".equals(item.getStatus())) {
                if (item.getFlowVersion() >= CURRENT_FLOW_VERSION) closePending(item, "EXPIRED");
                else refund(item, "EXPIRED");
                increaseUnread(item, item.getQuestioner().getId());
                increaseUnread(item, item.getAnswerer().getId());
                notifications.send(item.getQuestioner(), "询问已超时", notificationSubject(item) +
                    (item.getFlowVersion() >= CURRENT_FLOW_VERSION
                        ? "；对方72小时内未处理，询问已自动结束"
                        : "；冻结金额已退回余额"), "/inquiries/" + item.getId());
                notifications.send(item.getAnswerer(), "询问已超时", notificationSubject(item) +
                    "；72小时内未处理，询问已自动结束", "/inquiries/" + item.getId());
                publishInquiryChanged(item.getQuestioner().getId(), item);
                publishInquiryChanged(item.getAnswerer().getId(), item);
                analytics.recordBusinessAfterCommit(item.getQuestioner(), "inquiry_expired", java.util.Map.of(
                    "inquiry_id", item.getId(), "platform", item.getClientPlatform()
                ));
            }
        });
        jdbc.queryForList(
            "SELECT id FROM inquiries WHERE status='ACTIVE' AND reply_deadline<=?",
            Long.class,
            now
        ).forEach(id -> {
            Inquiry item = inquiries.findWithLockById(id).orElse(null);
            if (item != null && "ACTIVE".equals(item.getStatus()) && item.getReplyDeadline() != null
                && !item.getReplyDeadline().isAfter(LocalDateTime.now())) {
                processReplyTimeout(item);
            }
        });
        jdbc.queryForList(
            "SELECT i.id FROM inquiries i WHERE i.flow_version>=? " +
                "AND i.status IN ('ACTIVE','TEXT_LIMIT_REACHED') " +
                "AND i.conversation_expires_at IS NOT NULL AND i.conversation_expires_at<=? " +
                "AND NOT EXISTS (SELECT 1 FROM inquiry_voice_calls a " +
                "WHERE a.inquiry_id=i.id AND a.status IN ('CONNECTING','ACTIVE')) " +
                "ORDER BY i.conversation_expires_at ASC LIMIT 100",
            Long.class,
            CURRENT_FLOW_VERSION,
            now
        ).forEach(id -> {
            Inquiry item = inquiries.findWithLockById(id).orElse(null);
            if (item == null || item.getConversationExpiresAt() == null
                || item.getConversationExpiresAt().isAfter(LocalDateTime.now())
                || !Set.of("ACTIVE", "TEXT_LIMIT_REACHED").contains(item.getStatus())) {
                return;
            }
            closeWithoutFunds(item, "COMPLETED", "本次询问已达到20天期限，系统已自动结束。");
            increaseUnread(item, item.getQuestioner().getId());
            increaseUnread(item, item.getAnswerer().getId());
            notifications.send(item.getQuestioner(), "本次询问已结束", "已达到20天交流期限", "/inquiries/" + item.getId());
            notifications.send(item.getAnswerer(), "本次询问已结束", "已达到20天交流期限", "/inquiries/" + item.getId());
            publishInquiryChanged(item.getQuestioner().getId(), item);
            publishInquiryChanged(item.getAnswerer().getId(), item);
        });
    }

    private void processReplyTimeout(Inquiry item) {
        int timeoutNo = item.getTimeoutCount() + 1;
        if (timeoutNo > 5) return;
        BigDecimal targetTotal = timeoutNo == 5
            ? item.getAmount()
            : item.getAmount().multiply(BigDecimal.valueOf(timeoutNo)).divide(BigDecimal.valueOf(5));
        targetTotal = MoneyAmounts.normalize(targetTotal);
        BigDecimal refundAmount = MoneyAmounts.subtract(targetTotal, item.getTimeoutRefundedAmount());

        BigDecimal originalRecharge = MoneyAmounts.add(
            item.getFrozenRechargeAmount(), item.getTimeoutRefundedRechargeAmount()
        );
        BigDecimal originalIncome = MoneyAmounts.add(
            item.getFrozenIncomeAmount(), item.getTimeoutRefundedIncomeAmount()
        );
        BigDecimal targetRecharge = timeoutNo == 5
            ? originalRecharge
            : originalRecharge.multiply(BigDecimal.valueOf(timeoutNo))
                .divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);
        targetRecharge = targetRecharge.min(targetTotal);
        BigDecimal targetIncome = MoneyAmounts.subtract(targetTotal, targetRecharge);
        if (targetIncome.compareTo(originalIncome) > 0) {
            targetIncome = originalIncome;
            targetRecharge = MoneyAmounts.subtract(targetTotal, targetIncome);
        }
        BigDecimal rechargeRefund = MoneyAmounts.subtract(
            targetRecharge, item.getTimeoutRefundedRechargeAmount()
        );
        BigDecimal incomeRefund = MoneyAmounts.subtract(
            targetIncome, item.getTimeoutRefundedIncomeAmount()
        );

        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            wallet.refundInquiryTimeout(
                item.getQuestioner().getId(), rechargeRefund, incomeRefund, item.getId(), timeoutNo
            );
        }
        jdbc.update(
            "INSERT INTO inquiry_timeout_refunds(" +
                "inquiry_id,timeout_no,cycle_started_at,deadline_at,refund_amount," +
                "recharge_refund_amount,income_refund_amount) VALUES(?,?,?,?,?,?,?)",
            item.getId(), timeoutNo, item.getReplyCycleStartedAt(), item.getReplyDeadline(),
            refundAmount, rechargeRefund, incomeRefund
        );
        item.setTimeoutCount(timeoutNo);
        item.setTimeoutRefundedAmount(targetTotal);
        item.setTimeoutRefundedRechargeAmount(targetRecharge);
        item.setTimeoutRefundedIncomeAmount(targetIncome);
        item.setFrozenRechargeAmount(MoneyAmounts.subtract(item.getFrozenRechargeAmount(), rechargeRefund));
        item.setFrozenIncomeAmount(MoneyAmounts.subtract(item.getFrozenIncomeAmount(), incomeRefund));
        item.setSettleableAmount(MoneyAmounts.subtract(item.getAmount(), targetTotal));
        updateSettlementQuote(item);
        createSystemMessage(
            item,
            "TIMEOUT_NOTICE",
            "回答者已连续18小时未回复，本次询问已触发第" + timeoutNo +
                "次超时，已退回原始支付金额的20%。累计达到5次后，系统将自动关闭并全额退款。"
        );
        increaseUnread(item, item.getAnswerer().getId());
        notifications.send(
            item.getAnswerer(),
            "本次询问已触发超时",
            "第" + timeoutNo + "次超时，订单可结算金额已减少20%",
            "/inquiries/" + item.getId()
        );
        if (timeoutNo >= 5) {
            refundDeposit(item);
            item.setStatus("TIMEOUT_REFUNDED");
            item.setFundsStatus("REFUNDED");
            item.setEndedAt(LocalDateTime.now());
            item.setReplyCycleStartedAt(null);
            item.setReplyDeadline(null);
            item.setResponseDeadline(null);
            increaseUnread(item, item.getQuestioner().getId());
            notifications.send(
                item.getQuestioner(),
                "本次询问已自动结束",
                "回答者累计5次超时，询问金额已全部退回",
                "/inquiries/" + item.getId()
            );
        } else {
            item.setReplyDeadline(item.getReplyDeadline().plusHours(18));
        }
        publishInquiryChanged(item.getQuestioner().getId(), item);
        publishInquiryChanged(item.getAnswerer().getId(), item);
    }

    private void processOverdueBeforeReply(
        Inquiry item,
        Long senderId,
        LocalDateTime persistedAt
    ) {
        if (!item.getAnswerer().getId().equals(senderId)) return;
        while (
            "ACTIVE".equals(item.getStatus()) &&
                item.getReplyDeadline() != null &&
                !item.getReplyDeadline().isAfter(persistedAt) &&
                item.getTimeoutCount() < 5
        ) {
            processReplyTimeout(item);
        }
    }

    private void updateReplyCycle(Inquiry item, Long senderId, LocalDateTime sentAt) {
        if (item.getQuestioner().getId().equals(senderId)) {
            if (item.getFirstQuestionerMessageAt() == null) item.setFirstQuestionerMessageAt(sentAt);
            if (item.getReplyCycleStartedAt() == null) {
                item.setReplyCycleStartedAt(sentAt);
                item.setReplyDeadline(sentAt.plusHours(18));
            }
            return;
        }
        if (item.getFirstAnswererReplyAt() == null) item.setFirstAnswererReplyAt(sentAt);
        item.setReplyCycleStartedAt(null);
        item.setReplyDeadline(null);
    }

    private void requireConsecutiveMessageCapacity(Inquiry item, Long senderId) {
        if (messages.countConsecutiveMessages(item.getId(), senderId) >= 40) {
            throw BusinessException.badRequest("你已连续发送40条消息，请等待对方回复");
        }
    }

    private void requireTextMessageCapacity(Inquiry item, Long senderId) {
        long used = messages.countByInquiryIdAndSenderIdAndCountsTowardFreeLimitTrueAndMessageTypeIn(
            item.getId(), senderId, FREE_MESSAGE_TYPES
        );
        int limit = textMessageLimitFor(item, senderId);
        if (used >= limit) {
            throw BusinessException.badRequest("你的免费消息额度已用完");
        }
    }

    private void closeWhenTextLimitReached(Inquiry item, Long senderId) {
        long used = messages.countByInquiryIdAndSenderIdAndCountsTowardFreeLimitTrueAndMessageTypeIn(
            item.getId(), senderId, FREE_MESSAGE_TYPES
        );
        if (used < textMessageLimitFor(item, senderId)) return;
        refundDeposit(item);
        item.setStatus("TEXT_LIMIT_REACHED");
        item.setFundsStatus("NONE");
        item.setReplyCycleStartedAt(null);
        item.setReplyDeadline(null);
        createSystemMessage(item, "SYSTEM", "本次免费消息额度已用完，完成一次不少于5分钟的语音通话后，双方各增加50条文字消息。 ");
        increaseUnread(item, item.getQuestioner().getId().equals(senderId)
            ? item.getAnswerer().getId() : item.getQuestioner().getId());
        publishInquiryChanged(item.getQuestioner().getId(), item);
        publishInquiryChanged(item.getAnswerer().getId(), item);
    }

    private void updateFirstMessageTimes(Inquiry item, Long senderId, LocalDateTime sentAt) {
        if (item.getQuestioner().getId().equals(senderId) && item.getFirstQuestionerMessageAt() == null) {
            item.setFirstQuestionerMessageAt(sentAt);
        }
        if (item.getAnswerer().getId().equals(senderId) && item.getFirstAnswererReplyAt() == null) {
            item.setFirstAnswererReplyAt(sentAt);
        }
    }

    private void requireMessagingStatus(Inquiry item, Long senderId) {
        if ("ACTIVE".equals(item.getStatus())) return;
        throw BusinessException.badRequest("当前状态不能发送消息");
    }

    private InquiryCapacity inquiryCapacity() {
        InquiryCapacity capacity = jdbc.queryForObject(
            "SELECT questioner_active_limit,answerer_active_limit " +
                "FROM inquiry_capacity_settings WHERE id=1",
            (resultSet, rowNumber) -> new InquiryCapacity(
                resultSet.getInt("questioner_active_limit"),
                resultSet.getInt("answerer_active_limit")
            )
        );
        return capacity == null ? new InquiryCapacity(1, 3) : capacity;
    }

    private void requireInquiryCapacity(Long questionerId, Long answererId, InquiryCapacity capacity) {
        long questionerActive = inquiries.countByQuestionerIdAndStatusIn(
            questionerId,
            CAPACITY_OCCUPYING
        );
        if (questionerActive >= capacity.questionerLimit()) {
            throw BusinessException.badRequest(
                "你同时进行的询问已达" + capacity.questionerLimit() + "条，结束后才能发起新的询问"
            );
        }
        long answererActive = inquiries.countByAnswererIdAndStatusIn(
            answererId,
            CAPACITY_OCCUPYING
        );
        if (answererActive >= capacity.answererLimit()) {
            throw BusinessException.badRequest(
                "对方同时接受的询问已达" + capacity.answererLimit() + "条，请稍后再试"
            );
        }
    }

    private record InquiryCapacity(int questionerLimit, int answererLimit) {}

    private InquiryMessage createSystemMessage(Inquiry item, String type, String content) {
        InquiryMessage message = new InquiryMessage();
        message.setInquiry(item);
        message.setSender(null);
        message.setMessageType(type);
        message.setContent(content);
        message = messages.saveAndFlush(message);
        item.setLastMessageAt(message.getCreatedAt());
        MessageView view = messageView(message);
        realtime.afterCommit(item.getQuestioner().getId(), "INQUIRY_MESSAGE", new MessageEvent(
            item.getId(), unreadFor(item, item.getQuestioner().getId()), view
        ));
        realtime.afterCommit(item.getAnswerer().getId(), "INQUIRY_MESSAGE", new MessageEvent(
            item.getId(), unreadFor(item, item.getAnswerer().getId()), view
        ));
        return message;
    }

    private void refund(Inquiry item, String status) {
        wallet.refund(
            item.getQuestioner().getId(),
            item.getFrozenRechargeAmount(),
            item.getFrozenIncomeAmount(),
            item.getId()
        );
        item.setStatus(status); item.setFundsStatus("REFUNDED"); item.setResponseDeadline(null);
        item.setSettleableAmount(MoneyAmounts.ZERO);
        item.setReplyCycleStartedAt(null);
        item.setReplyDeadline(null);
        analytics.recordBusinessAfterCommit(item.getQuestioner(), "inquiry_refunded", java.util.Map.of(
            "inquiry_id", item.getId(),
            "reason", status,
            "platform", item.getClientPlatform(),
            "amount_bucket", amountBucket(item.getAmount())
        ));
    }

    private void refundDeposit(Inquiry item) {
        if (!"FROZEN".equals(item.getDepositStatus()) ||
            item.getDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        wallet.refundInquiryDeposit(
            item.getQuestioner().getId(),
            item.getDepositFrozenRechargeAmount(),
            item.getDepositFrozenIncomeAmount(),
            item.getId()
        );
        item.setDepositStatus("REFUNDED");
    }

    private void closePending(Inquiry item, String status) {
        if (item.getFlowVersion() < CURRENT_FLOW_VERSION && item.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            refund(item, status);
            refundDeposit(item);
            return;
        }
        closeWithoutFunds(item, status, null);
        item.setResponseDeadline(null);
    }

    private void closeWithoutFunds(Inquiry item, String status, String systemMessage) {
        refundDeposit(item);
        item.setStatus(status);
        item.setFundsStatus("NONE");
        item.setEndedAt(LocalDateTime.now());
        item.setResponseDeadline(null);
        item.setReplyCycleStartedAt(null);
        item.setReplyDeadline(null);
        if (systemMessage != null) createSystemMessage(item, "SYSTEM", systemMessage);
    }
    private void settle(Inquiry item) {
        updateSettlementQuote(item);
        wallet.settle(
            item.getQuestioner().getId(),
            item.getAnswerer().getId(),
            item.getFrozenRechargeAmount(),
            item.getFrozenIncomeAmount(),
            item
        );
        item.setStatus("COMPLETED"); item.setFundsStatus("SETTLED"); item.setEndedAt(LocalDateTime.now());
        item.setReplyCycleStartedAt(null);
        item.setReplyDeadline(null);
        increaseUnread(item, item.getAnswerer().getId());
        notifications.send(item.getAnswerer(), "交流已结束", notificationSubject(item) + "；费用已进入账户余额", "/inquiries/" + item.getId());
        analytics.recordBusinessAfterCommit(item.getQuestioner(), "inquiry_settled", java.util.Map.of(
            "inquiry_id", item.getId(),
            "answerer_id", item.getAnswerer().getId(),
            "platform", item.getClientPlatform(),
            "amount_bucket", amountBucket(item.getAmount())
        ));
    }

    private void updateSettlementQuote(Inquiry item) {
        if (item.getSettleableAmount().compareTo(BigDecimal.ZERO) == 0) {
            item.setServiceFeeAmount(MoneyAmounts.ZERO);
            item.setAnswererIncomeAmount(MoneyAmounts.ZERO);
            return;
        }
        var quote = wallet.quoteInquirySettlement(item.getSettleableAmount(), item.getClientPlatform());
        item.setClientPlatform(quote.clientPlatform());
        item.setServiceFeeRate(quote.serviceFeeRate());
        item.setServiceFeeAmount(quote.serviceFeeAmount());
        item.setAnswererIncomeAmount(quote.answererIncomeAmount());
    }
    private Inquiry lockedParticipant(Long userId, Long id, boolean answerer) {
        Inquiry item = inquiries.findWithLockById(id).orElseThrow(() -> BusinessException.notFound("询问不存在"));
        Long owner = answerer ? item.getAnswerer().getId() : item.getQuestioner().getId();
        if (!owner.equals(userId)) throw BusinessException.forbidden("无权执行该操作");
        return item;
    }
    private Inquiry accessible(Long userId, Long id) {
        Inquiry item = inquiries.findById(id).orElseThrow(() -> BusinessException.notFound("询问不存在"));
        if (!item.getQuestioner().getId().equals(userId) && !item.getAnswerer().getId().equals(userId))
            throw BusinessException.forbidden("无权查看该询问");
        return item;
    }
    private Inquiry lockedAccessible(Long userId, Long id) {
        Inquiry item = inquiries.findWithLockById(id).orElseThrow(() -> BusinessException.notFound("询问不存在"));
        if (!item.getQuestioner().getId().equals(userId) && !item.getAnswerer().getId().equals(userId))
            throw BusinessException.forbidden("无权查看该询问");
        return item;
    }
    private void increaseUnread(Inquiry item, Long userId) {
        if (item.getQuestioner().getId().equals(userId)) {
            item.setQuestionerUnreadCount(item.getQuestionerUnreadCount() + 1);
        } else if (item.getAnswerer().getId().equals(userId)) {
            item.setAnswererUnreadCount(item.getAnswererUnreadCount() + 1);
        }
    }
    private int unreadFor(Inquiry item, Long userId) {
        return item.getQuestioner().getId().equals(userId)
            ? item.getQuestionerUnreadCount()
            : item.getAnswererUnreadCount();
    }
    private void publishInquiryChanged(Long userId, Inquiry item) {
        realtime.afterCommit(userId, "INQUIRY_UPDATED", new InquiryChangedEvent(
            item.getId(), item.getStatus(), item.getFundsStatus(), unreadFor(item, userId)
        ));
    }
    private User user(Long id) { return users.findById(id).orElseThrow(() -> BusinessException.notFound("用户不存在")); }
    private User lockedUser(Long id) {
        return users.findWithLockById(id).orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }
    private void lockParticipants(Inquiry item) {
        Long questionerId = item.getQuestioner().getId();
        Long answererId = item.getAnswerer().getId();
        if (questionerId < answererId) {
            lockedUser(questionerId);
            lockedUser(answererId);
        } else {
            lockedUser(answererId);
            lockedUser(questionerId);
        }
    }
    private LockedParticipants lockParticipants(Long questionerId, Long answererId) {
        if (questionerId < answererId) {
            return new LockedParticipants(lockedUser(questionerId), lockedUser(answererId));
        }
        User answerer = lockedUser(answererId);
        User questioner = lockedUser(questionerId);
        return new LockedParticipants(questioner, answerer);
    }
    private record LockedParticipants(User questioner, User answerer) {}
    private void requireStatus(Inquiry i, String status) { if (!status.equals(i.getStatus())) throw BusinessException.badRequest("当前状态不能执行该操作"); }
    static String required(String s, String message, int max) {
        if (s == null || s.isBlank()) throw BusinessException.badRequest(message);
        String value = s.trim();
        if (value.length() > max) {
            throw BusinessException.badRequest(message.equals("消息不能为空")
                ? "每条消息最多" + max + "字"
                : "最多输入" + max + "字");
        }
        return value;
    }
    private String clean(String s, int max) { if (s == null) return null; String v = s.trim(); return v.length() <= max ? v : v.substring(0, max); }
    private void validateChatImage(MultipartFile image) {
        if (image == null || image.isEmpty()) throw BusinessException.badRequest("请选择照片");
        if (image.getSize() > 10L * 1024 * 1024) throw BusinessException.badRequest("照片不能超过10MB");
        String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw BusinessException.badRequest("只能发送照片");
        }
    }
    private String displayName(User user) {
        return user.getNickname() == null || user.getNickname().isBlank() ? "UID " + user.getUid() : user.getNickname();
    }
    private String notificationSubject(Inquiry item) {
        String question = item.getQuestion() == null ? "这次询问" : item.getQuestion().trim();
        return question.length() <= 36 ? question : question.substring(0, 36) + "…";
    }

    private String amountBucket(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("20")) <= 0) return "1-20";
        if (amount.compareTo(new BigDecimal("50")) <= 0) return "21-50";
        if (amount.compareTo(new BigDecimal("100")) <= 0) return "51-100";
        if (amount.compareTo(new BigDecimal("300")) <= 0) return "101-300";
        if (amount.compareTo(new BigDecimal("1000")) <= 0) return "301-1000";
        return "1001-5000";
    }

    private String normalizePlatform(String platform) {
        String value = platform == null ? "ANDROID" : platform.trim().toUpperCase();
        return Set.of("ANDROID", "IOS").contains(value) ? value : "ANDROID";
    }

    private AppGlobalSettingService.Settings settings() {
        return globalSettings == null ? null : globalSettings.current();
    }

    private String storagePrefix(User user) {
        return "TEST".equals(user.getAccountType()) ? "test/" : "";
    }

    private MessageView messageView(InquiryMessage message) {
        return MessageView.of(message, fileStorage);
    }

    private InquiryView view(Inquiry i, Long me) {
        User other = i.getQuestioner().getId().equals(me) ? i.getAnswerer() : i.getQuestioner();
        boolean communicationBlocked = isCommunicationBlocked(
            i.getQuestioner().getId(),
            i.getAnswerer().getId()
        );
        int questionerTextCount = Math.toIntExact(messages.countByInquiryIdAndSenderIdAndCountsTowardFreeLimitTrueAndMessageTypeIn(
            i.getId(), i.getQuestioner().getId(), FREE_MESSAGE_TYPES
        ));
        int answererTextCount = Math.toIntExact(messages.countByInquiryIdAndSenderIdAndCountsTowardFreeLimitTrueAndMessageTypeIn(
            i.getId(), i.getAnswerer().getId(), FREE_MESSAGE_TYPES
        ));
        return new InquiryView(i.getId(), i.getQuestioner().getId().equals(me) ? "QUESTIONER" : "ANSWERER",
                other.getId(), other.getNickname() == null || other.getNickname().isBlank() ? other.getUid() : other.getNickname(),
                other.getAvatarUrl(), i.getTopic(), i.getQuestion(), sourceExperienceCertificationId(i),
                i.getAmount(), i.getDepositAmount(), i.getDepositStatus(), i.getSettleableAmount(),
                i.getTimeoutRefundedAmount(), i.getTimeoutCount(), i.getServiceFeeRate(),
                i.getServiceFeeAmount(), i.getAnswererIncomeAmount(), i.getStatus(), i.getFundsStatus(),
                unreadFor(i, me), i.getResponseDeadline(), i.getConversationExpiresAt(), i.getCreatedAt(),
                i.getLastMessageAt(), i.getFirstAnswererReplyAt(),
                i.getFlowVersion(), i.getHourlyRateSnapshot(),
                questionerTextCount, answererTextCount,
                i.getQuestioner().getId().equals(me) ? i.getQuestionerTextLimit() : i.getAnswererTextLimit(),
                communicationBlocked);
    }

    private int textMessageLimitFor(Inquiry inquiry, Long userId) {
        int configured = inquiry.getQuestioner().getId().equals(userId)
            ? inquiry.getQuestionerTextLimit()
            : inquiry.getAnswererTextLimit();
        return configured > 0 ? configured : INITIAL_TEXT_MESSAGE_LIMIT;
    }

    private void requireCommunicationAllowed(Inquiry inquiry) {
        requireCommunicationAllowed(
            inquiry.getQuestioner().getId(),
            inquiry.getAnswerer().getId()
        );
    }

    private void requireCommunicationAllowed(Long firstUserId, Long secondUserId) {
        communicationBlocks.requireCommunicationAllowed(firstUserId, secondUserId);
    }

    private boolean isCommunicationBlocked(Long firstUserId, Long secondUserId) {
        return communicationBlocks.isBlocked(firstUserId, secondUserId);
    }

    private Long sourceExperienceCertificationId(Inquiry inquiry) {
        if (inquiry.getSourceExperienceCertificationId() != null) {
            return inquiry.getSourceExperienceCertificationId();
        }
        List<Long> matched = jdbc.queryForList(
            "SELECT id FROM certifications WHERE user_id=? AND category='EXPERIENCE' " +
                "AND status='APPROVED' AND enabled=TRUE AND deleted_at IS NULL AND title=? " +
                "ORDER BY id DESC LIMIT 1",
            Long.class,
            inquiry.getAnswerer().getId(),
            inquiry.getTopic()
        );
        return matched.isEmpty() ? null : matched.get(0);
    }
    public record CreateCommand(
        Long answererId,
        Long sourceExperienceCertificationId,
        String question,
        String clientPlatform
    ) {}
    public record InquiryView(Long id, String role, Long otherUserId, String otherName, String otherAvatar, String topic,
                              String question, Long sourceExperienceCertificationId,
                              BigDecimal amount, BigDecimal depositAmount, String depositStatus,
                              BigDecimal settleableAmount,
                              BigDecimal timeoutRefundedAmount, int timeoutCount, BigDecimal serviceFeeRate,
                              BigDecimal serviceFeeAmount, BigDecimal answererIncomeAmount,
                              String status, String fundsStatus,
                              int unreadCount, LocalDateTime responseDeadline, LocalDateTime conversationExpiresAt,
                              LocalDateTime createdAt, LocalDateTime lastMessageAt,
                              LocalDateTime firstAnswererReplyAt,
                              int flowVersion, int hourlyRateSnapshot,
                              int questionerTextCount, int answererTextCount, int textMessageLimit,
                              boolean communicationBlocked) {}
    public record MessageView(Long id, Long senderId, String senderName, String senderAvatar, String type, String content,
                              String attachmentUrl, String attachmentName, Long attachmentSize,
                              LocalDateTime createdAt) {
        static MessageView of(InquiryMessage m, FileStorage fileStorage) {
            User sender = m.getSender();
            String attachmentUrl = m.getAttachmentKey() == null || m.getAttachmentKey().isBlank()
                ? m.getAttachmentUrl()
                : fileStorage.accessUrl(m.getAttachmentKey(), StorageVisibility.PRIVATE);
            return new MessageView(
                m.getId(),
                sender == null ? null : sender.getId(),
                sender == null ? "系统消息" : sender.getNickname() == null || sender.getNickname().isBlank()
                    ? sender.getUid()
                    : sender.getNickname(),
                sender == null ? null : sender.getAvatarUrl(),
                m.getMessageType(),
                m.getContent(),
                attachmentUrl,
                m.getAttachmentName(),
                m.getAttachmentSize(),
                m.getCreatedAt()
            );
        }
    }
    public record InquiryDetail(InquiryView inquiry, List<MessageView> messages) {}
    public record InquiryChangedEvent(Long inquiryId, String status, String fundsStatus, int unreadCount) {}
    public record MessageEvent(Long inquiryId, int unreadCount, MessageView message) {}
}
