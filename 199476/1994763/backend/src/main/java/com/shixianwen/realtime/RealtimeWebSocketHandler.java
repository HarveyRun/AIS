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
    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<WebSocketSession>> adminSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        sessions(session).computeIfAbsent(subjectId(session), ignored -> new CopyOnWriteArraySet<>()).add(session);
        send(session, new RealtimeEvent("CONNECTED", Map.of(), Instant.now()));
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

    public record RealtimeEvent(String type, Object payload, Instant sentAt) {}
}
