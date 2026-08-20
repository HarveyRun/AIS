package com.shixianwen.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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
        send(session, new RealtimeEvent("CONNECTED", Map.of(), Instant.now()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        session.close(CloseStatus.POLICY_VIOLATION.withReason("不接受客户端消息"));
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
