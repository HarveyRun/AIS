package com.shixianwen.inquiry;

import com.shixianwen.common.BusinessException;
import com.shixianwen.config.AppGlobalSettingService;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.MoneyAmounts;
import com.shixianwen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InquiryVoiceCallService {
    private static final Set<String> CALLABLE_INQUIRY_STATUSES = Set.of(
        "ACTIVE", "TEXT_LIMIT_REACHED"
    );
    private static final Set<String> OPEN_CALL_STATUSES = Set.of(
        "CONNECTING", "ACTIVE"
    );
    private static final int CONNECT_TIMEOUT_SECONDS = 60;
    private static final int RECONNECT_TIMEOUT_SECONDS = 60;
    private static final int REWARD_TEXT_SECONDS = 5 * 60;
    private static final int REWARD_TEXT_MESSAGES = 50;

    private final InquiryVoiceCallRepository voiceCalls;
    private final InquiryRepository inquiries;
    private final InquiryMessageRepository messages;
    private final UserRepository users;
    private final WalletService wallet;
    private final NotificationService notifications;
    private final RealtimePublisher realtime;
    private final UserCommunicationBlockService communicationBlocks;
    private final JdbcTemplate jdbc;
    @Autowired(required = false)
    private AppGlobalSettingService globalSettings;

    @Transactional(readOnly = true)
    public VoiceCallView latest(Long userId, Long inquiryId) {
        Inquiry inquiry = accessibleInquiry(userId, inquiryId);
        return voiceCalls.findTopByInquiryIdOrderByIdDesc(inquiry.getId())
            .map(item -> view(item, userId))
            .orElseGet(() -> VoiceCallView.empty(inquiry.getId()));
    }

    @Transactional
    public VoiceCallView create(Long userId, Long inquiryId) {
        AppGlobalSettingService.Settings settings = settings();
        if (settings != null && !settings.voiceCallEnabled()) {
            throw BusinessException.serviceUnavailable("语音通话暂时不可用");
        }
        Inquiry inquiry = lockedInquiry(inquiryId);
        requireQuestioner(inquiry, userId);
        requireCommunicationAllowed(inquiry);
        if (inquiry.getFlowVersion() < 2 || !CALLABLE_INQUIRY_STATUSES.contains(inquiry.getStatus())) {
            throw BusinessException.badRequest("当前状态不能发起语音通话");
        }
        if (voiceCalls.existsByInquiryIdAndStatusIn(inquiryId, OPEN_CALL_STATUSES)) {
            throw BusinessException.badRequest("当前已有一条待处理或进行中的语音通话");
        }
        int hourlyRate = inquiry.getHourlyRateSnapshot();
        int minimumRate = settings == null ? 1 : settings.hourlyRateMin();
        int maximumRate = settings == null ? 5000 : settings.hourlyRateMax();
        if (hourlyRate < minimumRate || hourlyRate > maximumRate) {
            throw BusinessException.badRequest("对方尚未设置每小时费用");
        }
        if (!realtime.isUserOnline(inquiry.getAnswerer().getId())) {
            throw BusinessException.badRequest("对方当前不在线");
        }

        LocalDateTime now = LocalDateTime.now();
        InquiryVoiceCall call = new InquiryVoiceCall();
        call.setInquiry(inquiry);
        call.setQuestioner(inquiry.getQuestioner());
        call.setAnswerer(inquiry.getAnswerer());
        call.setMaxEndAt(inquiryDeadline(inquiry, now.plusDays(settings == null ? 20 : settings.inquiryMaxDurationDays())));
        call.setHourlyRateSnapshot(hourlyRate);
        call.setReservedAmount(MoneyAmounts.ZERO);
        call.setActualAmount(MoneyAmounts.ZERO);
        call.setStatus("CONNECTING");
        int ringSeconds = settings == null ? CONNECT_TIMEOUT_SECONDS : settings.voiceRingTimeoutSeconds();
        call.setConnectDeadline(now.plusSeconds(ringSeconds));
        call.setQuestionerJoinedAt(now);
        call = voiceCalls.saveAndFlush(call);

        WalletService.FrozenAllocation allocation = wallet.reserveVoiceCall(userId, call.getId());
        BigDecimal reserve = MoneyAmounts.add(allocation.rechargeAmount(), allocation.incomeAmount());
        call.setReservedAmount(reserve);
        call.setFrozenRechargeAmount(allocation.rechargeAmount());
        call.setFrozenIncomeAmount(allocation.incomeAmount());
        call.setServiceFeeRate(wallet.quoteInquirySettlement(reserve, inquiry.getClientPlatform()).serviceFeeRate());
        call.setMaxEndAt(inquiryDeadline(inquiry, now.plusSeconds(maxBillableSeconds(reserve, hourlyRate))));

        recordVoiceEvent(call, userId, "CALL_STARTED", null);
        notifications.send(
            inquiry.getAnswerer(), "语音来电", "对方向你发起了语音通话",
            "/inquiries/" + inquiry.getId()
        );
        realtime.afterCommit(call.getAnswerer().getId(), "VOICE_CALL_STARTED", java.util.Map.of(
            "inquiryId", inquiryId, "voiceCallId", call.getId(),
            "questionerId", call.getQuestioner().getId()
        ));
        return view(call, userId);
    }

    @Transactional
    public VoiceCallView answer(Long userId, Long inquiryId, Long voiceCallId) {
        InquiryVoiceCall call = lockedVoiceCall(inquiryId, voiceCallId);
        requireAnswerer(call, userId);
        requireCommunicationAllowed(call.getInquiry());
        requireStatus(call, "CONNECTING");
        if (call.getAcceptedAt() != null) return view(call, userId);
        LocalDateTime now = LocalDateTime.now();
        if (call.getConnectDeadline() == null || !call.getConnectDeadline().isAfter(now)) {
            expireConnection(call);
            return view(call, userId);
        }
        lockParticipants(call.getInquiry());
        call.setAcceptedAt(now);
        call.setAnswererJoinedAt(now);
        AppGlobalSettingService.Settings settings = settings();
        call.setConnectDeadline(now.plusSeconds(settings == null ? CONNECT_TIMEOUT_SECONDS : settings.voiceRingTimeoutSeconds()));
        recordVoiceEvent(call, userId, "CALL_ANSWERED", null);
        realtime.afterCommit(call.getQuestioner().getId(), "VOICE_CALL_ANSWERED", java.util.Map.of(
            "inquiryId", inquiryId, "voiceCallId", voiceCallId
        ));
        return view(call, userId);
    }

    @Transactional
    public VoiceCallView reject(Long userId, Long inquiryId, Long voiceCallId) {
        InquiryVoiceCall call = lockedVoiceCall(inquiryId, voiceCallId);
        requireAnswerer(call, userId);
        requireStatus(call, "CONNECTING");
        if (call.getAcceptedAt() != null) {
            throw BusinessException.badRequest("语音通话已经接听");
        }
        refund(call, "REJECTED");
        createSystemMessage(call.getInquiry(), "对方未接听语音通话，冻结金额已退回余额。");
        increaseUnread(call.getInquiry(), call.getQuestioner().getId());
        publishChanged(call.getInquiry());
        publishVoiceEvent(call, "VOICE_CALL_ENDED");
        return view(call, userId);
    }

    @Transactional
    public VoiceCallView joinCall(Long userId, Long inquiryId, Long voiceCallId) {
        InquiryVoiceCall call = lockedVoiceCall(inquiryId, voiceCallId);
        requireParticipant(call, userId);
        requireOpenConnection(call);
        if (call.getAnswerer().getId().equals(userId) && call.getAcceptedAt() == null) {
            throw BusinessException.badRequest("请先接听语音通话");
        }
        LocalDateTime now = LocalDateTime.now();
        if (call.getQuestioner().getId().equals(userId)) call.setQuestionerJoinedAt(now);
        else call.setAnswererJoinedAt(now);
        recordVoiceEvent(call, userId, "PARTICIPANT_JOINED", null);
        return view(call, userId);
    }

    @Transactional
    public VoiceCallView connected(Long userId, Long inquiryId, Long voiceCallId) {
        InquiryVoiceCall call = lockedVoiceCall(inquiryId, voiceCallId);
        requireParticipant(call, userId);
        requireOpenConnection(call);
        if (call.getAnswerer().getId().equals(userId) && call.getAcceptedAt() == null) {
            throw BusinessException.badRequest("请先接听语音通话");
        }
        LocalDateTime now = LocalDateTime.now();
        if (call.getQuestioner().getId().equals(userId)) call.setQuestionerConnectedAt(now);
        else call.setAnswererConnectedAt(now);
        call.setReconnectDeadline(null);
        recordVoiceEvent(call, userId, "PEER_CONNECTED", null);
        if ("CONNECTING".equals(call.getStatus())
            && call.getQuestionerConnectedAt() != null
            && call.getAnswererConnectedAt() != null) {
            call.setConnectedAt(now);
            call.setActiveSegmentStartedAt(now);
            call.setMaxEndAt(inquiryDeadline(
                call.getInquiry(),
                now.plusSeconds(maxBillableSeconds(call.getReservedAmount(), call.getHourlyRateSnapshot()))
            ));
            activate(call, now);
            publishVoiceEvent(call, "VOICE_CALL_CONNECTED");
        } else if ("ACTIVE".equals(call.getStatus())
            && call.getQuestionerConnectedAt() != null
            && call.getAnswererConnectedAt() != null
            && call.getActiveSegmentStartedAt() == null) {
            call.setActiveSegmentStartedAt(now);
            publishVoiceEvent(call, "VOICE_CALL_RECONNECTED");
        }
        return view(call, userId);
    }

    @Transactional
    public VoiceCallView disconnected(Long userId, Long inquiryId, Long voiceCallId, String reason) {
        InquiryVoiceCall call = lockedVoiceCall(inquiryId, voiceCallId);
        requireParticipant(call, userId);
        if (!Set.of("CONNECTING", "ACTIVE").contains(call.getStatus())) return view(call, userId);
        LocalDateTime now = LocalDateTime.now();
        closeActiveSegment(call, now);
        if (call.getQuestioner().getId().equals(userId)) call.setQuestionerConnectedAt(null);
        else call.setAnswererConnectedAt(null);
        call.setLastDisconnectedAt(now);
        AppGlobalSettingService.Settings settings = settings();
        call.setReconnectDeadline(now.plusSeconds(settings == null ? RECONNECT_TIMEOUT_SECONDS : settings.voiceReconnectTimeoutSeconds()));
        recordVoiceEvent(call, userId, "PEER_DISCONNECTED", limit(reason, 500));
        return view(call, userId);
    }

    @Transactional
    public VoiceCallView finishCall(Long userId, Long inquiryId, Long voiceCallId) {
        InquiryVoiceCall call = lockedVoiceCall(inquiryId, voiceCallId);
        requireParticipant(call, userId);
        if ("COMPLETED".equals(call.getStatus())) return view(call, userId);
        if ("CONNECTING".equals(call.getStatus())) {
            refund(call, "CANCELLED");
            createSystemMessage(call.getInquiry(), "语音通话未接通，冻结金额已退回余额。");
            publishChanged(call.getInquiry());
            publishVoiceEvent(call, "VOICE_CALL_ENDED");
            return view(call, userId);
        }
        requireStatus(call, "ACTIVE");
        call.setEndReason(call.getQuestioner().getId().equals(userId)
            ? "QUESTIONER_HANGUP" : "ANSWERER_HANGUP");
        recordVoiceEvent(call, userId, "CALL_FINISHED", call.getEndReason());
        complete(call, LocalDateTime.now());
        return view(call, userId);
    }

    @Scheduled(fixedDelayString = "${app.inquiry.voice-call-scan-ms:30000}")
    @Transactional
    public void processVoiceCalls() {
        LocalDateTime now = LocalDateTime.now();
        jdbc.queryForList(
            "SELECT id FROM inquiries WHERE flow_version>=2 " +
                "AND status IN ('ACTIVE','TEXT_LIMIT_REACHED') " +
                "AND conversation_expires_at IS NOT NULL AND conversation_expires_at<=? " +
                "ORDER BY conversation_expires_at ASC LIMIT 100",
            Long.class, now
        ).forEach(id -> inquiries.findWithLockById(id).ifPresent(inquiry -> {
            if (inquiry.getConversationExpiresAt() != null && !inquiry.getConversationExpiresAt().isAfter(now)) {
                forceEndExpiredInquiry(inquiry, now);
            }
        }));
        voiceCalls.findTop100ByStatusAndConnectDeadlineBeforeOrderByConnectDeadlineAsc("CONNECTING", now)
            .forEach(candidate -> voiceCalls.findWithLockById(candidate.getId()).ifPresent(call -> {
                if ("CONNECTING".equals(call.getStatus()) && call.getConnectDeadline() != null
                    && !call.getConnectDeadline().isAfter(now)) expireConnection(call);
            }));
        voiceCalls.findTop100ByStatusAndReconnectDeadlineBeforeOrderByReconnectDeadlineAsc("ACTIVE", now)
            .forEach(candidate -> voiceCalls.findWithLockById(candidate.getId()).ifPresent(call -> {
                if ("ACTIVE".equals(call.getStatus()) && call.getReconnectDeadline() != null
                    && !call.getReconnectDeadline().isAfter(now)) {
                    call.setEndReason("RECONNECT_TIMEOUT");
                    complete(call, now);
                }
            }));
        voiceCalls.findTop100ByStatusAndMaxEndAtBeforeOrderByMaxEndAtAsc("ACTIVE", now)
            .forEach(candidate -> voiceCalls.findWithLockById(candidate.getId()).ifPresent(call -> {
                if ("ACTIVE".equals(call.getStatus()) && !call.getMaxEndAt().isAfter(now)) {
                    call.setEndReason("BALANCE_OR_INQUIRY_LIMIT");
                    complete(call, now);
                }
            }));
    }

    private void activate(InquiryVoiceCall call, LocalDateTime now) {
        Inquiry inquiry = call.getInquiry();
        if (!CALLABLE_INQUIRY_STATUSES.contains(inquiry.getStatus())) {
            refund(call, "CANCELLED");
            return;
        }
        refundInquiryDeposit(inquiry);
        call.setStatus("ACTIVE");
        call.setConnectDeadline(null);
        call.setReconnectDeadline(null);
        createSystemMessage(inquiry, "语音通话已接通，按实际通话时间计费。");
        increaseUnread(inquiry, inquiry.getQuestioner().getId());
        increaseUnread(inquiry, inquiry.getAnswerer().getId());
        publishChanged(inquiry);
    }

    private void complete(InquiryVoiceCall call, LocalDateTime now) {
        if (!"ACTIVE".equals(call.getStatus())) return;
        Inquiry inquiry = call.getInquiry();
        closeActiveSegment(call, now);
        long seconds = call.getBillableSeconds();
        seconds = Math.min(seconds, maxBillableSeconds(call.getReservedAmount(), call.getHourlyRateSnapshot()));
        BigDecimal actualAmount = calculateAmount(call.getHourlyRateSnapshot(), seconds);
        if (actualAmount.compareTo(call.getReservedAmount()) > 0) actualAmount = call.getReservedAmount();
        BigDecimal serviceFee = MoneyAmounts.normalize(actualAmount.multiply(call.getServiceFeeRate()));
        BigDecimal answererIncome = MoneyAmounts.subtract(actualAmount, serviceFee);

        call.setActualDurationSeconds(Math.toIntExact(Math.min(seconds, Integer.MAX_VALUE)));
        call.setActualAmount(actualAmount);
        call.setServiceFeeAmount(serviceFee);
        call.setAnswererIncomeAmount(answererIncome);
        inquiry.setAmount(actualAmount);
        inquiry.setSettleableAmount(actualAmount);
        inquiry.setServiceFeeRate(call.getServiceFeeRate());
        inquiry.setServiceFeeAmount(serviceFee);
        inquiry.setAnswererIncomeAmount(answererIncome);
        // Hibernate may auto-flush the inquiry before WalletService locks the two
        // wallet rows. Clear the inquiry's reserved-source snapshot first so the
        // database invariant remains true after amount changes to the actual charge.
        inquiry.setFrozenRechargeAmount(MoneyAmounts.ZERO);
        inquiry.setFrozenIncomeAmount(MoneyAmounts.ZERO);

        wallet.settleVoiceCall(
            inquiry.getQuestioner().getId(), inquiry.getAnswerer().getId(),
            call.getFrozenRechargeAmount(), call.getFrozenIncomeAmount(), actualAmount,
            inquiry, call.getId()
        );
        AppGlobalSettingService.Settings settings = settings();
        int rewardSeconds = settings == null ? REWARD_TEXT_SECONDS : settings.voiceRewardSeconds();
        int rewardMessages = settings == null ? REWARD_TEXT_MESSAGES : settings.voiceRewardMessages();
        boolean quotaAdded = seconds >= rewardSeconds && !call.isTextQuotaGranted();
        if (quotaAdded) {
            inquiry.setQuestionerTextLimit(inquiry.getQuestionerTextLimit() + rewardMessages);
            inquiry.setAnswererTextLimit(inquiry.getAnswererTextLimit() + rewardMessages);
            call.setTextQuotaGranted(true);
        }
        long questionerUsed = textMessages(inquiry, inquiry.getQuestioner().getId());
        long answererUsed = textMessages(inquiry, inquiry.getAnswerer().getId());
        inquiry.setStatus(questionerUsed >= inquiry.getQuestionerTextLimit()
            || answererUsed >= inquiry.getAnswererTextLimit() ? "TEXT_LIMIT_REACHED" : "ACTIVE");
        inquiry.setFundsStatus(actualAmount.compareTo(BigDecimal.ZERO) > 0 ? "SETTLED" : "REFUNDED");
        call.setStatus("COMPLETED");
        call.setEndedAt(now);

        String durationText = formatDuration(seconds);
        String text = "语音通话已结束，实际通话" + durationText + "，实际支付"
            + actualAmount.stripTrailingZeros().toPlainString() + "元。";
        if (quotaAdded) text += "双方各增加" + rewardMessages + "条文字消息。";
        createSystemMessage(inquiry, text);
        increaseUnread(inquiry, inquiry.getQuestioner().getId());
        increaseUnread(inquiry, inquiry.getAnswerer().getId());
        notifications.send(inquiry.getQuestioner(), "语音通话已结束", text, "/inquiries/" + inquiry.getId());
        notifications.send(
            inquiry.getAnswerer(), "语音通话已结束",
            "实际收入" + answererIncome.stripTrailingZeros().toPlainString() + "元",
            "/inquiries/" + inquiry.getId()
        );
        publishChanged(inquiry);
        publishVoiceEvent(call, "VOICE_CALL_ENDED");
    }

    private void expireConnection(InquiryVoiceCall call) {
        call.setEndReason("CONNECT_TIMEOUT");
        recordVoiceEvent(call, null, "CONNECT_TIMEOUT", null);
        refund(call, "CONNECTION_FAILED");
        createSystemMessage(call.getInquiry(), "对方未接听语音通话，冻结金额已退回余额。");
        increaseUnread(call.getInquiry(), call.getQuestioner().getId());
        increaseUnread(call.getInquiry(), call.getAnswerer().getId());
        publishChanged(call.getInquiry());
        publishVoiceEvent(call, "VOICE_CALL_ENDED");
    }

    private void refund(InquiryVoiceCall call, String status) {
        wallet.refundVoiceCall(
            call.getQuestioner().getId(), call.getFrozenRechargeAmount(),
            call.getFrozenIncomeAmount(), call.getId()
        );
        call.setStatus(status);
        call.setEndReason(status);
        call.setEndedAt(LocalDateTime.now());
    }

    private void refundInquiryDeposit(Inquiry inquiry) {
        if (!"FROZEN".equals(inquiry.getDepositStatus())
            || inquiry.getDepositAmount().compareTo(BigDecimal.ZERO) <= 0) return;
        wallet.refundInquiryDeposit(
            inquiry.getQuestioner().getId(), inquiry.getDepositFrozenRechargeAmount(),
            inquiry.getDepositFrozenIncomeAmount(), inquiry.getId()
        );
        inquiry.setDepositStatus("REFUNDED");
    }

    private void forceEndExpiredInquiry(Inquiry inquiry, LocalDateTime now) {
        voiceCalls.findTopByInquiryIdOrderByIdDesc(inquiry.getId()).ifPresent(call -> {
            if ("CONNECTING".equals(call.getStatus())) {
                refund(call, "EXPIRED");
            } else if ("ACTIVE".equals(call.getStatus())) {
                call.setEndReason("INQUIRY_EXPIRED");
                complete(call, now);
            }
        });
        if (!Set.of("ACTIVE", "TEXT_LIMIT_REACHED").contains(inquiry.getStatus())) return;
        refundInquiryDeposit(inquiry);
        inquiry.setStatus("COMPLETED");
        inquiry.setEndedAt(now);
        createSystemMessage(inquiry, "本次询问已达到20天期限，系统已自动结束。");
        increaseUnread(inquiry, inquiry.getQuestioner().getId());
        increaseUnread(inquiry, inquiry.getAnswerer().getId());
        publishChanged(inquiry);
    }

    private BigDecimal calculateAmount(int hourlyRate, long seconds) {
        if (seconds <= 0) return MoneyAmounts.ZERO;
        return BigDecimal.valueOf(hourlyRate)
            .multiply(BigDecimal.valueOf(seconds))
            .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
    }

    private long maxBillableSeconds(BigDecimal reserve, int hourlyRate) {
        return reserve.multiply(BigDecimal.valueOf(3600))
            .divide(BigDecimal.valueOf(hourlyRate), 0, RoundingMode.FLOOR)
            .max(BigDecimal.ONE).min(BigDecimal.valueOf(20L * 24 * 3600)).longValueExact();
    }

    private LocalDateTime inquiryDeadline(Inquiry inquiry, LocalDateTime fallback) {
        LocalDateTime deadline = inquiry.getConversationExpiresAt();
        return deadline != null && deadline.isBefore(fallback) ? deadline : fallback;
    }

    private long textMessages(Inquiry inquiry, Long userId) {
        return messages.countByInquiryIdAndSenderIdAndCountsTowardFreeLimitTrueAndMessageTypeIn(
            inquiry.getId(), userId, Set.of("TEXT")
        );
    }

    private void closeActiveSegment(InquiryVoiceCall call, LocalDateTime now) {
        LocalDateTime startedAt = call.getActiveSegmentStartedAt();
        if (startedAt == null) return;
        long seconds = Math.max(0, Duration.between(startedAt, now).getSeconds());
        long total = Math.min((long) call.getBillableSeconds() + seconds, Integer.MAX_VALUE);
        call.setBillableSeconds((int) total);
        call.setActiveSegmentStartedAt(null);
    }

    private InquiryVoiceCall lockedVoiceCall(Long inquiryId, Long voiceCallId) {
        InquiryVoiceCall call = voiceCalls.findWithLockById(voiceCallId)
            .orElseThrow(() -> BusinessException.notFound("语音通话不存在"));
        if (!call.getInquiry().getId().equals(inquiryId)) {
            throw BusinessException.notFound("语音通话不存在");
        }
        return call;
    }

    private Inquiry accessibleInquiry(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiries.findById(inquiryId)
            .orElseThrow(() -> BusinessException.notFound("询问不存在"));
        if (!inquiry.getQuestioner().getId().equals(userId)
            && !inquiry.getAnswerer().getId().equals(userId)) {
            throw BusinessException.forbidden("无权查看该询问");
        }
        return inquiry;
    }

    private Inquiry lockedInquiry(Long inquiryId) {
        return inquiries.findWithLockById(inquiryId)
            .orElseThrow(() -> BusinessException.notFound("询问不存在"));
    }

    private void requireQuestioner(Inquiry inquiry, Long userId) {
        if (!inquiry.getQuestioner().getId().equals(userId)) {
            throw BusinessException.forbidden("只有提问者可以发起语音通话");
        }
    }

    private void requireAnswerer(InquiryVoiceCall call, Long userId) {
        if (!call.getAnswerer().getId().equals(userId)) {
            throw BusinessException.forbidden("只有回答者可以处理语音通话");
        }
    }

    private void requireParticipant(InquiryVoiceCall call, Long userId) {
        if (!call.getQuestioner().getId().equals(userId)
            && !call.getAnswerer().getId().equals(userId)) {
            throw BusinessException.forbidden("无权进入该语音通话");
        }
    }

    private void requireStatus(InquiryVoiceCall call, String expected) {
        if (!expected.equals(call.getStatus())) {
            throw BusinessException.badRequest("当前语音通话状态不能执行该操作");
        }
    }

    private void requireOpenConnection(InquiryVoiceCall call) {
        if (!Set.of("CONNECTING", "ACTIVE").contains(call.getStatus())) {
            throw BusinessException.badRequest("本次语音通话已不可用");
        }
    }

    private void requireCommunicationAllowed(Inquiry inquiry) {
        communicationBlocks.requireCommunicationAllowed(
            inquiry.getQuestioner().getId(), inquiry.getAnswerer().getId()
        );
    }

    private void lockParticipants(Inquiry inquiry) {
        Long first = inquiry.getQuestioner().getId();
        Long second = inquiry.getAnswerer().getId();
        if (first < second) {
            lockedUser(first);
            lockedUser(second);
        } else {
            lockedUser(second);
            lockedUser(first);
        }
    }

    private User lockedUser(Long userId) {
        return users.findWithLockById(userId)
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }

    private void recordVoiceEvent(InquiryVoiceCall call, Long userId, String type, String detail) {
        jdbc.update(
            "INSERT INTO voice_call_events(inquiry_id,voice_call_id,user_id,event_type,detail) VALUES(?,?,?,?,?)",
            call.getInquiry().getId(), call.getId(), userId, type, detail
        );
    }

    private void publishVoiceEvent(InquiryVoiceCall call, String type) {
        var payload = java.util.Map.of(
            "inquiryId", call.getInquiry().getId(), "voiceCallId", call.getId()
        );
        realtime.afterCommit(call.getQuestioner().getId(), type, payload);
        realtime.afterCommit(call.getAnswerer().getId(), type, payload);
    }

    private void createSystemMessage(Inquiry inquiry, String content) {
        InquiryMessage message = new InquiryMessage();
        message.setInquiry(inquiry);
        message.setSender(null);
        message.setMessageType("SYSTEM");
        message.setContent(content);
        message = messages.saveAndFlush(message);
        inquiry.setLastMessageAt(message.getCreatedAt());
    }

    private void increaseUnread(Inquiry inquiry, Long recipientId) {
        if (inquiry.getQuestioner().getId().equals(recipientId)) {
            inquiry.setQuestionerUnreadCount(inquiry.getQuestionerUnreadCount() + 1);
        } else {
            inquiry.setAnswererUnreadCount(inquiry.getAnswererUnreadCount() + 1);
        }
    }

    private void publishChanged(Inquiry inquiry) {
        realtime.afterCommit(inquiry.getQuestioner().getId(), "INQUIRY_UPDATED", java.util.Map.of(
            "inquiryId", inquiry.getId(), "status", inquiry.getStatus()
        ));
        realtime.afterCommit(inquiry.getAnswerer().getId(), "INQUIRY_UPDATED", java.util.Map.of(
            "inquiryId", inquiry.getId(), "status", inquiry.getStatus()
        ));
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        String text = value.trim();
        return text.length() <= max ? text : text.substring(0, max);
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long rest = seconds % 60;
        if (hours > 0) return hours + "小时" + minutes + "分" + rest + "秒";
        if (minutes > 0) return minutes + "分" + rest + "秒";
        return rest + "秒";
    }

    private AppGlobalSettingService.Settings settings() {
        return globalSettings == null ? null : globalSettings.current();
    }

    private VoiceCallView view(InquiryVoiceCall call, Long userId) {
        return VoiceCallView.of(call, userId);
    }

    public record VoiceCallView(
        Long id, Long inquiryId, String role, String status,
        LocalDateTime maxEndAt, int actualDurationSeconds, int hourlyRateSnapshot,
        BigDecimal reservedAmount, BigDecimal actualAmount, BigDecimal answererIncomeAmount,
        LocalDateTime acceptedAt, LocalDateTime connectDeadline,
        LocalDateTime connectedAt, LocalDateTime reconnectDeadline,
        String endReason, LocalDateTime createdAt
    ) {
        static VoiceCallView of(InquiryVoiceCall call, Long userId) {
            return new VoiceCallView(
                call.getId(), call.getInquiry().getId(),
                call.getAnswerer().getId().equals(userId) ? "ANSWERER" : "QUESTIONER",
                call.getStatus(), call.getMaxEndAt(), call.getActualDurationSeconds(),
                call.getHourlyRateSnapshot(), call.getReservedAmount(), call.getActualAmount(),
                call.getAnswererIncomeAmount(), call.getAcceptedAt(), call.getConnectDeadline(),
                call.getConnectedAt(), call.getReconnectDeadline(), call.getEndReason(), call.getCreatedAt()
            );
        }

        static VoiceCallView empty(Long inquiryId) {
            return new VoiceCallView(
                null, inquiryId, "", "NONE", null, 0, 0,
                MoneyAmounts.ZERO, MoneyAmounts.ZERO, MoneyAmounts.ZERO,
                null, null, null, null, null, null
            );
        }
    }
}
