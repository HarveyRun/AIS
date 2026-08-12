package com.shixianwen.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class RealtimeWebSocketConfiguration implements WebSocketConfigurer {
    private final RealtimeWebSocketHandler handler;
    private final RealtimeHandshakeInterceptor handshakeInterceptor;
    private final String[] allowedOrigins;

    public RealtimeWebSocketConfiguration(
        RealtimeWebSocketHandler handler,
        RealtimeHandshakeInterceptor handshakeInterceptor,
        @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        this.handler = handler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/realtime/ws")
            .addInterceptors(handshakeInterceptor)
            .setAllowedOrigins(allowedOrigins);
    }
}
