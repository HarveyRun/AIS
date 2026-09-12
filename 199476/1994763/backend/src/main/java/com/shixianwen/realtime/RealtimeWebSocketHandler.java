package com.shixianwen.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@RequiredArgsConstructor
public class RealtimeWebSocketHandler extends TextWebSocketHandler {
    private static final long ONLINE_HEARTBEAT_TIMEOUT_MILLIS = 35_000L;
    private static final long STALE_SESSION_TIMEOUT_MILLIS = 90_000L;
    private static final int MAX_SESSIONS_PER_SUBJECT = 3;
    private static final int MAX_SESSIONS_PER_IP = 20;
    private static final int MAX_TOTAL_SESSIONS = 5000;
    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<WebSocketSession>> adminSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        Map<Long, Set<WebSocketSession>> target = sessions(session);
        Set<WebSocketSession> subjectConnections = target.computeIfAbsent(
            subjectId(session),
            ignored -> new CopyOnWriteArraySet<>()
        );
        if (subjectConnections.size() >= MAX_SESSIONS_PER_SUBJECT
            || totalSessions() >= MAX_TOTAL_SESSIONS
            || sessionsForIp(String.valueOf(session.getAttributes().get("clientIp"))) >= MAX_SESSIONS_PER_IP) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("连接数量已达上限"));
            return;
        }
        subjectConnections.add(session);
        if (!"ADMIN".equals(session.getAttributes().get("subjectType"))) {
            session.getAttributes().put("lastHeartbeatAt", System.currentTimeMillis());
            session.getAttributes().put("appForeground", true);
        }
        send(session, new RealtimeEvent("CONNECTED", Map.of(), Instant.now()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ADMIN".equals(session.getAttributes().get("subjectType"))) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("不接受客户端消息"));
            return;
        }
        Map<?, ?> payload;
        try {
            payload = objectMapper.readValue(message.getPayload(), Map.class);
        } catch (Exception exception) {
            session.close(CloseStatus.BAD_DATA.withReason("消息格式无效"));
            return;
        }
        if (!"HEARTBEAT".equals(payload.get("type"))) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("不接受此客户端消息"));
            return;
        }
        session.getAttributes().put("lastHeartbeatAt", System.currentTimeMillis());
        session.getAttributes().put("appForeground", Boolean.TRUE.equals(payload.get("foreground")));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Map<Long, Set<WebSocketSession>> subjectSessions = sessions(session);
        Long subjectId = subjectId(session);
        Set<WebSocketSession> connectedSessions = subjectSessions.get(subjectId);
        if (connectedSessions == null) return;
        connectedSessions.remove(session);
        if (connectedSessions.isEmpty()) subjectSessions.remove(subjectId);
    }

    public void send(Long userId, String type, Object payload) {
        send(userSessions.get(userId), type, payload);
    }

    public void sendAdmins(String type, Object payload) {
        adminSessions.values().forEach(connectedSessions -> send(connectedSessions, type, payload));
    }

    public void sendAllUsers(String type, Object payload) {
        userSessions.values().forEach(
            connectedSessions -> send(connectedSessions, type, payload)
        );
    }

    public boolean isUserOnline(Long userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        long threshold = System.currentTimeMillis() - ONLINE_HEARTBEAT_TIMEOUT_MILLIS;
        return sessions != null && sessions.stream().anyMatch(session ->
            session.isOpen()
                && Boolean.TRUE.equals(session.getAttributes().get("appForeground"))
                && heartbeatAt(session) >= threshold
        );
    }

    @Scheduled(fixedDelay = 30_000L)
    public void closeStaleUserSessions() {
        long threshold = System.currentTimeMillis() - STALE_SESSION_TIMEOUT_MILLIS;
        userSessions.values().stream().flatMap(Set::stream).toList().forEach(session -> {
            if (heartbeatAt(session) >= threshold) return;
            try { session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("心跳超时")); }
            catch (IOException ignored) { }
        });
    }

    private long heartbeatAt(WebSocketSession session) {
        Object value = session.getAttributes().get("lastHeartbeatAt");
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private void send(Set<WebSocketSession> connectedSessions, String type, Object payload) {
        if (connectedSessions == null) return;
        RealtimeEvent event = new RealtimeEvent(type, payload, Instant.now());
        connectedSessions.forEach(session -> {
            try {
                send(session, event);
            } catch (IOException exception) {
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException ignored) {
                }
            }
        });
    }

    private void send(WebSocketSession session, RealtimeEvent event) throws IOException {
        if (!session.isOpen()) return;
        synchronized (session) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
        }
    }

    private Map<Long, Set<WebSocketSession>> sessions(WebSocketSession session) {
        return "ADMIN".equals(session.getAttributes().get("subjectType"))
            ? adminSessions
            : userSessions;
    }

    private static Long subjectId(WebSocketSession session) {
        return (Long) session.getAttributes().get("subjectId");
    }

    private int totalSessions() {
        return countSessions(userSessions) + countSessions(adminSessions);
    }

    private int sessionsForIp(String ip) {
        return countSessionsForIp(userSessions, ip) + countSessionsForIp(adminSessions, ip);
    }

    private int countSessions(Map<Long, Set<WebSocketSession>> source) {
        return source.values().stream().mapToInt(Set::size).sum();
    }

    private int countSessionsForIp(Map<Long, Set<WebSocketSession>> source, String ip) {
        return source.values().stream()
            .flatMap(Set::stream)
            .map(session -> String.valueOf(session.getAttributes().get("clientIp")))
            .mapToInt(value -> value.equals(ip) ? 1 : 0)
            .sum();
    }

    public record RealtimeEvent(String type, Object payload, Instant sentAt) {}
}
