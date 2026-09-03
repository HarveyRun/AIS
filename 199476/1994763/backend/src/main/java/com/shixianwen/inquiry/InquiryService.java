package com.shixianwen.inquiry;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.common.BusinessException;
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
    private static final Set<String> OPEN = Set.of("PENDING", "ACTIVE", "AWAITING_CONFIRMATION", "DISPUTED");
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

    @Transactional
    public InquiryView create(Long questionerId, CreateCommand command, ClientNetworkInfo network) {
        if (questionerId.equals(command.answererId())) throw BusinessException.badRequest("不能向自己发起询问");
        User questioner = user(questionerId);
        User answerer = user(command.answererId());
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
        if (inquiries.existsByQuestionerIdAndAnswererIdAndStatusIn(questionerId, answerer.getId(), OPEN))
            throw BusinessException.badRequest("你们已有一条进行中的询问");
        List<InquiryExperience> matchedExperiences = jdbc.query(
            "SELECT title FROM certifications WHERE id=? AND user_id=? " +
                "AND category='EXPERIENCE' AND COALESCE(experience_business_type,'MONETIZED')='MONETIZED' " +
                "AND status='APPROVED' AND enabled=TRUE AND deleted_at IS NULL",
            (resultSet, rowNumber) -> new InquiryExperience(resultSet.getString("title")),
            command.sourceExperienceCertificationId(),
            answerer.getId()
        );
        if (matchedExperiences.isEmpty()) {
            throw BusinessException.badRequest("这段亲身经历已不可询问");
        }
        InquiryExperience selectedExperience = matchedExperiences.get(0);
        BigDecimal amount = MoneyAmounts.requireWholeAmount(
            command.amount(), BigDecimal.ONE, new BigDecimal("5000"), "询问金额"
        );
        requirePriceInRange(answerer, amount);
        var settlementQuote = wallet.quoteInquirySettlement(amount, command.clientPlatform());
        String topic = selectedExperience.title();
        Inquiry item = new Inquiry();
        item.setQuestioner(questioner); item.setAnswerer(answerer); item.setTopic(sensitiveWords.mask(topic));
        item.setSourceType("EXPERIENCE");
        item.setSourceExperienceCertificationId(command.sourceExperienceCertificationId());
        item.setQuestion(sensitiveWords.mask(topic));
        item.setQuestionRawEncrypted(sensitiveContentCipher.encrypt(topic));
        item.setRequestIp(network.ipAddress());
        item.setRequestLocation(network.location());
        item.setAmount(amount);
        item.setSettleableAmount(amount);
        item.setClientPlatform(settlementQuote.clientPlatform());
        item.setServiceFeeRate(settlementQuote.serviceFeeRate());
        item.setServiceFeeAmount(settlementQuote.serviceFeeAmount());
        item.setAnswererIncomeAmount(settlementQuote.answererIncomeAmount());
        item.setStatus("PENDING"); item.setFundsStatus("FROZEN");
        item.setAnswererUnreadCount(1);
        item.setResponseDeadline(LocalDateTime.now().plusHours(24));
        item = inquiries.save(item);
        WalletService.FrozenAllocation allocation = wallet.freeze(questionerId, item.getAmount(), item.getId());
        item.setFrozenRechargeAmount(allocation.rechargeAmount());
        item.setFrozenIncomeAmount(allocation.incomeAmount());
        notifications.send(answerer, "收到新的询问", displayName(questioner) + "：" + notificationSubject(item), "/inquiries/" + item.getId());
        publishInquiryChanged(answerer.getId(), item);
        analytics.recordBusinessAfterCommit(questioner, "inquiry_created", java.util.Map.of(
            "inquiry_id", item.getId(),
            "answerer_id", answerer.getId(),
            "source_type", item.getSourceType(),
            "platform", item.getClientPlatform(),
            "amount_bucket", amountBucket(item.getAmount())
        ));
        return view(item, questionerId);
    }

    private void requirePriceInRange(User answerer, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(answerer.getInquiryPriceMin())) < 0
            || amount.compareTo(BigDecimal.valueOf(answerer.getInquiryPriceMax())) > 0) {
            throw BusinessException.badRequest(
                "对方可接受的询问金额为¥" + answerer.getInquiryPriceMin() + "—¥" +
                    answerer.getInquiryPriceMax()
            );
        }
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
        requireStatus(item, "PENDING");
        answererEligibility.requireCanAccept(userId);
        item.setAnswererUnreadCount(0);
        item.setStatus("ACTIVE"); item.setAcceptedAt(LocalDateTime.now()); item.setResponseDeadline(null);
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
        refund(item, "REJECTED");
        increaseUnread(item, item.getQuestioner().getId());
        notifications.send(item.getQuestioner(), "询问未被接受", notificationSubject(item) + "；冻结金额已退回余额", "/inquiries/" + item.getId());
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
        refund(item, "CANCELLED");
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
        requireMessagingStatus(item, userId);
        requireQuestionerFirstTurn(item, userId);
        requireConsecutiveMessageCapacity(item, userId);
        chatAbuseGuard.requireTextAllowed(inquiryId, userId, content == null ? "" : content.trim());
        if (item.getQuestioner().getId().equals(userId)) item.setQuestionerUnreadCount(0);
        else item.setAnswererUnreadCount(0);
        InquiryMessage message = new InquiryMessage();
        message.setInquiry(item); message.setSender(user(userId));
        String originalContent = required(content, "消息不能为空", 500);
        message.setRawContentEncrypted(sensitiveContentCipher.encrypt(originalContent));
        message.setContent(sensitiveWords.mask(originalContent));
        message = messages.saveAndFlush(message);
        item.setLastMessageAt(message.getCreatedAt());
        processOverdueBeforeReply(item, userId, message.getCreatedAt());
        if ("ACTIVE".equals(item.getStatus())) {
            updateReplyCycle(item, userId, message.getCreatedAt());
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
        requireMessagingStatus(item, userId);
        requireQuestionerFirstTurn(item, userId);
        requireConsecutiveMessageCapacity(item, userId);
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
        message.setContent("");
        message.setAttachmentKey(stored.storageKey());
        message.setAttachmentUrl(null);
        message.setAttachmentName(clean(image.getOriginalFilename(), 255));
        message.setAttachmentSize(stored.size());
        message = messages.saveAndFlush(message);
        item.setLastMessageAt(message.getCreatedAt());
        processOverdueBeforeReply(item, userId, message.getCreatedAt());
        if ("ACTIVE".equals(item.getStatus())) {
            updateReplyCycle(item, userId, message.getCreatedAt());
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
    public InquiryView requestEnd(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, true);
        requireStatus(item, "ACTIVE");
        if (item.getFirstAnswererReplyAt() == null) {
            throw BusinessException.badRequest("发送回复后才能申请结束");
        }
        if (item.getFirstAnswererReplyAt().plusHours(72).isAfter(LocalDateTime.now())) {
            throw BusinessException.badRequest("满72小时后才能申请结束");
        }
        item.setAnswererUnreadCount(0);
        LocalDateTime now = LocalDateTime.now();
        item.setStatus("AWAITING_CONFIRMATION");
        item.setEndRequestedAt(now);
        item.setEndReminderStage(0);
        item.setConfirmationDeadline(now.plusHours(6));
        item.setReplyCycleStartedAt(null);
        item.setReplyDeadline(null);
        createSystemMessage(item, "SYSTEM", "回答者已申请结束本次交流。申请提交后不能撤回，双方暂时无法继续发送消息。");
        increaseUnread(item, item.getQuestioner().getId());
        notifications.send(item.getQuestioner(), "对方申请结束交流", notificationSubject(item) + "；请同意结束或提出不同意", "/inquiries/" + item.getId());
        publishInquiryChanged(item.getQuestioner().getId(), item);
        return view(item, userId);
    }

    @Transactional
    public InquiryView disagreeEnd(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, false);
        requireStatus(item, "AWAITING_CONFIRMATION");
        item.setQuestionerUnreadCount(0);
        createEndDispute(item, "QUESTIONER_DISAGREED");
        increaseUnread(item, item.getAnswerer().getId());
        notifications.send(item.getAnswerer(), "本次交流进入平台处理", notificationSubject(item) + "；提问者不同意结束", "/inquiries/" + item.getId());
        publishInquiryChanged(item.getAnswerer().getId(), item);
        return view(item, userId);
    }

    @Transactional
    public InquiryView confirmEnd(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, false);
        if (!Set.of("ACTIVE", "AWAITING_CONFIRMATION").contains(item.getStatus()))
            throw BusinessException.badRequest("当前状态不能结束交流");
        item.setQuestionerUnreadCount(0);
        settle(item);
        publishInquiryChanged(item.getAnswerer().getId(), item);
        return view(item, userId);
    }

    @Transactional
    public void resolveEndDisputeFunds(Long inquiryId, boolean settleRemaining) {
        Inquiry item = inquiries.findWithLockById(inquiryId)
            .orElseThrow(() -> BusinessException.notFound("询问不存在"));
        if (!"DISPUTED".equals(item.getStatus()) || !"END_DISPUTE".equals(item.getFundsStatus())) {
            throw BusinessException.badRequest("该询问不在结束纠纷处理中");
        }
        if (settleRemaining) {
            settle(item);
            notifications.send(item.getQuestioner(), "平台处理完成", "本次询问剩余金额已结算", "/inquiries/" + item.getId());
        } else {
            wallet.refund(
                item.getQuestioner().getId(),
                item.getFrozenRechargeAmount(),
                item.getFrozenIncomeAmount(),
                item.getId()
            );
            item.setStatus("END_DISPUTE_REFUNDED");
            item.setFundsStatus("REFUNDED");
            item.setSettleableAmount(MoneyAmounts.ZERO);
            item.setServiceFeeAmount(MoneyAmounts.ZERO);
            item.setAnswererIncomeAmount(MoneyAmounts.ZERO);
            notifications.send(item.getQuestioner(), "平台处理完成", "本次询问剩余金额已退回余额", "/inquiries/" + item.getId());
            notifications.send(item.getAnswerer(), "平台处理完成", "本次询问剩余金额已退回提问者", "/inquiries/" + item.getId());
        }
        publishInquiryChanged(item.getQuestioner().getId(), item);
        publishInquiryChanged(item.getAnswerer().getId(), item);
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
                refund(item, "EXPIRED");
                increaseUnread(item, item.getQuestioner().getId());
                increaseUnread(item, item.getAnswerer().getId());
                notifications.send(item.getQuestioner(), "询问已超时", notificationSubject(item) + "；冻结金额已退回余额", "/inquiries/" + item.getId());
                notifications.send(item.getAnswerer(), "询问已超时", notificationSubject(item) + "；已自动关闭", "/inquiries/" + item.getId());
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
            "SELECT id FROM inquiries WHERE status='AWAITING_CONFIRMATION' AND confirmation_deadline<=?",
            Long.class,
            now
        ).forEach(id -> {
            Inquiry item = inquiries.findWithLockById(id).orElse(null);
            if (item != null && "AWAITING_CONFIRMATION".equals(item.getStatus())) {
                processEndReminder(item);
            }
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

        wallet.refundInquiryTimeout(
            item.getQuestioner().getId(), rechargeRefund, incomeRefund, item.getId(), timeoutNo
        );
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
            item.setStatus("TIMEOUT_REFUNDED");
            item.setFundsStatus("REFUNDED");
            item.setEndedAt(LocalDateTime.now());
            item.setReplyCycleStartedAt(null);
            item.setReplyDeadline(null);
            item.setResponseDeadline(null);
            item.setConfirmationDeadline(null);
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

    private void processEndReminder(Inquiry item) {
        int stage = item.getEndReminderStage();
        if (stage == 0) {
            item.setEndReminderStage(1);
            item.setConfirmationDeadline(item.getEndRequestedAt().plusHours(12));
            createSystemMessage(item, "SYSTEM", "结束申请已等待6小时，请提问者及时处理。");
            increaseUnread(item, item.getQuestioner().getId());
            notifications.send(item.getQuestioner(), "请处理结束申请", "结束申请已等待6小时", "/inquiries/" + item.getId());
        } else if (stage == 1) {
            item.setEndReminderStage(2);
            item.setConfirmationDeadline(item.getEndRequestedAt().plusHours(24));
            createSystemMessage(item, "SYSTEM", "结束申请已等待12小时，请提问者及时处理；24小时未处理将交由平台处理。");
            increaseUnread(item, item.getQuestioner().getId());
            notifications.send(item.getQuestioner(), "结束申请即将交由平台处理", "结束申请已等待12小时", "/inquiries/" + item.getId());
        } else {
            createEndDispute(item, "QUESTIONER_NO_RESPONSE");
            notifications.send(item.getQuestioner(), "本次交流已结束", "结束申请已交由平台处理", "/inquiries/" + item.getId());
            notifications.send(item.getAnswerer(), "本次交流已结束", "结束申请已交由平台处理", "/inquiries/" + item.getId());
        }
        publishInquiryChanged(item.getQuestioner().getId(), item);
        publishInquiryChanged(item.getAnswerer().getId(), item);
    }

    private void createEndDispute(Inquiry item, String triggerType) {
        jdbc.update(
            "INSERT IGNORE INTO inquiry_end_disputes(inquiry_id,trigger_type) VALUES(?,?)",
            item.getId(), triggerType
        );
        item.setStatus("DISPUTED");
        item.setFundsStatus("END_DISPUTE");
        item.setEndedAt(LocalDateTime.now());
        item.setConfirmationDeadline(null);
        item.setReplyCycleStartedAt(null);
        item.setReplyDeadline(null);
        createSystemMessage(item, "SYSTEM", "本次订单已结束，剩余金额继续冻结，等待平台处理纠纷。");
        recordEvidence(item.getId(), "END_DISPUTE_CREATED", "SYSTEM", null, triggerType);
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

    private void requireQuestionerFirstTurn(Inquiry item, Long senderId) {
        if (!item.getQuestioner().getId().equals(senderId)) return;
        if (item.getFirstAnswererReplyAt() != null) return;
        if (messages.countByInquiryIdAndSenderId(item.getId(), senderId) >= 1) {
            throw BusinessException.badRequest("请等待对方回复后再继续发送");
        }
    }

    private void requireMessagingStatus(Inquiry item, Long senderId) {
        if ("ACTIVE".equals(item.getStatus())) return;
        if ("PENDING".equals(item.getStatus()) && item.getQuestioner().getId().equals(senderId)) {
            return;
        }
        throw BusinessException.badRequest("当前状态不能发送消息");
    }

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

    private void recordEvidence(Long inquiryId, String eventType, String actorType, Long actorId, String detail) {
        String cleanDetail = clean(detail, 1000);
        String hash = sha256(inquiryId + "|" + eventType + "|" + actorType + "|" + actorId + "|" + cleanDetail);
        jdbc.update(
            "INSERT INTO inquiry_dispute_events(inquiry_id,event_type,actor_type,actor_id,detail,evidence_hash) " +
                "VALUES(?,?,?,?,?,?)",
            inquiryId, eventType, actorType, actorId, cleanDetail, hash
        );
    }

    private String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
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
        item.setConfirmationDeadline(null);
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
    private void requireStatus(Inquiry i, String status) { if (!status.equals(i.getStatus())) throw BusinessException.badRequest("当前状态不能执行该操作"); }
    private String required(String s, String message, int max) { if (s == null || s.isBlank()) throw BusinessException.badRequest(message); return clean(s, max); }
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

    private String storagePrefix(User user) {
        return "TEST".equals(user.getAccountType()) ? "test/" : "";
    }

    private MessageView messageView(InquiryMessage message) {
        return MessageView.of(message, fileStorage);
    }

    private InquiryView view(Inquiry i, Long me) {
        User other = i.getQuestioner().getId().equals(me) ? i.getAnswerer() : i.getQuestioner();
        return new InquiryView(i.getId(), i.getQuestioner().getId().equals(me) ? "QUESTIONER" : "ANSWERER",
                other.getId(), other.getNickname() == null || other.getNickname().isBlank() ? other.getUid() : other.getNickname(),
                other.getAvatarUrl(), i.getTopic(), i.getQuestion(), i.getAmount(), i.getSettleableAmount(),
                i.getTimeoutRefundedAmount(), i.getTimeoutCount(), i.getServiceFeeRate(),
                i.getServiceFeeAmount(), i.getAnswererIncomeAmount(), i.getStatus(), i.getFundsStatus(),
                unreadFor(i, me), i.getResponseDeadline(), i.getConfirmationDeadline(), i.getCreatedAt(),
                i.getLastMessageAt(), i.getFirstAnswererReplyAt(), i.getEndRequestedAt());
    }
    public record CreateCommand(
        Long answererId,
        Long sourceExperienceCertificationId,
        BigDecimal amount,
        String clientPlatform
    ) {}
    public record InquiryView(Long id, String role, Long otherUserId, String otherName, String otherAvatar, String topic,
                              String question, BigDecimal amount, BigDecimal settleableAmount,
                              BigDecimal timeoutRefundedAmount, int timeoutCount, BigDecimal serviceFeeRate,
                              BigDecimal serviceFeeAmount, BigDecimal answererIncomeAmount,
                              String status, String fundsStatus,
                              int unreadCount, LocalDateTime responseDeadline, LocalDateTime confirmationDeadline,
                              LocalDateTime createdAt, LocalDateTime lastMessageAt,
                              LocalDateTime firstAnswererReplyAt, LocalDateTime endRequestedAt) {}
    public record MessageView(Long id, Long senderId, String senderName, String senderAvatar, String type, String content,
                              String attachmentUrl, String attachmentName, Long attachmentSize,
                              LocalDateTime createdAt, boolean reportable) {
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
                m.getCreatedAt(),
                sender != null && Set.of("TEXT", "IMAGE").contains(m.getMessageType())
            );
        }
    }
    public record InquiryDetail(InquiryView inquiry, List<MessageView> messages) {}
    public record InquiryChangedEvent(Long inquiryId, String status, String fundsStatus, int unreadCount) {}
    public record MessageEvent(Long inquiryId, int unreadCount, MessageView message) {}
}
