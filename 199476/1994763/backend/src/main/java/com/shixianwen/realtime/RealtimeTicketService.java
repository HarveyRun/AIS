package com.shixianwen.realtime;

import com.shixianwen.common.BusinessException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RealtimeTicketService {
    private static final long VALID_SECONDS = 30;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public TicketView issue(Long userId) {
        cleanup();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plusSeconds(VALID_SECONDS);
        tickets.put(value, new Ticket(userId, expiresAt));
        return new TicketView(value, expiresAt);
    }

    public Long consume(String value) {
        Ticket ticket = value == null ? null : tickets.remove(value);
        if (ticket == null || ticket.expiresAt().isBefore(Instant.now())) {
            throw BusinessException.forbidden("实时连接凭证无效");
        }
        return ticket.userId();
    }

    private void cleanup() {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record Ticket(Long userId, Instant expiresAt) {}
    public record TicketView(String ticket, Instant expiresAt) {}
}
