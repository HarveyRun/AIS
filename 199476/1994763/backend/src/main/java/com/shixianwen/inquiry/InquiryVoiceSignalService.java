package com.shixianwen.inquiry;

import com.shixianwen.common.BusinessException;
import com.shixianwen.realtime.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InquiryVoiceSignalService {
    private static final Set<String> SIGNAL_TYPES = Set.of("OFFER", "ANSWER", "ICE", "HANGUP");
    private static final int MAX_SIGNAL_LENGTH = 100_000;

    private final InquiryVoiceCallRepository voiceCalls;
    private final JdbcTemplate jdbc;
    private final RealtimePublisher realtime;

    @Value("${app.voice.ice-urls:stun:stun.l.google.com:19302}")
    private String iceUrls;

    @Value("${app.voice.turn-username:}")
    private String turnUsername;

    @Value("${app.voice.turn-credential:}")
    private String turnCredential;

    @Transactional(readOnly = true)
    public IceConfig iceConfig(Long userId, Long inquiryId, Long voiceCallId) {
        requireActiveParticipant(userId, inquiryId, voiceCallId);
        List<String> urls = Arrays.stream(iceUrls.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
        if (urls.isEmpty()) urls = List.of("stun:stun.l.google.com:19302");
        return new IceConfig(urls, turnUsername, turnCredential);
    }

    @Transactional
    public SignalView send(
        Long userId,
        Long inquiryId,
        Long voiceCallId,
        String signalType,
        String payload
    ) {
        InquiryVoiceCall voiceCall = requireActiveParticipant(userId, inquiryId, voiceCallId);
        String type = signalType == null ? "" : signalType.trim().toUpperCase();
        if (!SIGNAL_TYPES.contains(type)) {
            throw BusinessException.badRequest("语音通话信令不正确");
        }
        String content = payload == null ? "" : payload.trim();
        if (content.isBlank() || content.length() > MAX_SIGNAL_LENGTH) {
            throw BusinessException.badRequest("语音通话信令内容不正确");
        }
        Long recipientId = voiceCall.getQuestioner().getId().equals(userId)
            ? voiceCall.getAnswerer().getId()
            : voiceCall.getQuestioner().getId();
        jdbc.update(
            "INSERT INTO inquiry_voice_signals(" +
                "inquiry_id,voice_call_id,sender_id,recipient_id,signal_type,payload" +
                ") VALUES(?,?,?,?,?,?)",
            inquiryId,
            voiceCallId,
            userId,
            recipientId,
            type,
            content
        );
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        SignalView signal = new SignalView(
            id == null ? 0L : id,
            inquiryId,
            voiceCallId,
            userId,
            type,
            content,
            LocalDateTime.now()
        );
        realtime.afterCommit(recipientId, "VOICE_SIGNAL", signal);
        return signal;
    }

    @Transactional(readOnly = true)
    public List<SignalView> pending(Long userId, Long inquiryId, Long voiceCallId, long afterId) {
        InquiryVoiceCall voiceCall = requireActiveParticipant(userId, inquiryId, voiceCallId);
        LocalDateTime startedAt = voiceCall.getConnectedAt();
        return jdbc.query(
            "SELECT id,inquiry_id,voice_call_id,sender_id,signal_type,payload,created_at " +
                "FROM inquiry_voice_signals WHERE voice_call_id=? AND recipient_id=? " +
                "AND id>? AND created_at>=? ORDER BY id ASC LIMIT 500",
            (resultSet, rowNumber) -> new SignalView(
                resultSet.getLong("id"),
                resultSet.getLong("inquiry_id"),
                resultSet.getLong("voice_call_id"),
                resultSet.getLong("sender_id"),
                resultSet.getString("signal_type"),
                resultSet.getString("payload"),
                resultSet.getObject("created_at", LocalDateTime.class)
            ),
            voiceCallId,
            userId,
            Math.max(0L, afterId),
            startedAt == null ? LocalDateTime.now().minusMinutes(5) : startedAt.minusSeconds(2)
        );
    }

    private InquiryVoiceCall requireActiveParticipant(
        Long userId,
        Long inquiryId,
        Long voiceCallId
    ) {
        InquiryVoiceCall voiceCall = voiceCalls.findDetailById(voiceCallId)
            .orElseThrow(() -> BusinessException.notFound("语音通话不存在"));
        if (!voiceCall.getInquiry().getId().equals(inquiryId)) {
            throw BusinessException.notFound("语音通话不存在");
        }
        boolean participant = voiceCall.getQuestioner().getId().equals(userId)
            || voiceCall.getAnswerer().getId().equals(userId);
        if (!participant) throw BusinessException.forbidden("无权进入该语音通话");
        if (!Set.of("CONNECTING", "ACTIVE").contains(voiceCall.getStatus())
            || voiceCall.getMaxEndAt() == null
            || !voiceCall.getMaxEndAt().isAfter(LocalDateTime.now())) {
            throw BusinessException.badRequest("本次语音通话已不可用");
        }
        return voiceCall;
    }

    @Scheduled(fixedDelayString = "${app.voice.signal-cleanup-ms:3600000}")
    public void cleanupExpiredSignals() {
        jdbc.update(
            "DELETE FROM inquiry_voice_signals WHERE created_at<? LIMIT 5000",
            LocalDateTime.now().minusDays(1)
        );
    }

    public record IceConfig(List<String> urls, String username, String credential) {}

    public record SignalView(
        Long id,
        Long inquiryId,
        Long voiceCallId,
        Long senderId,
        String signalType,
        String payload,
        LocalDateTime createdAt
    ) {}
}
