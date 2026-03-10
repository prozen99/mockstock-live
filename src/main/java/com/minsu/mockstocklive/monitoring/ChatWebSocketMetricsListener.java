package com.minsu.mockstocklive.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketMetricsListener {

    private final Set<String> activeSessionIds = ConcurrentHashMap.newKeySet();

    public ChatWebSocketMetricsListener(MeterRegistry meterRegistry) {
        Gauge.builder("mockstock.chat.websocket.sessions.active", activeSessionIds, Set::size)
                .description("Current active WebSocket chat sessions")
                .register(meterRegistry);
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        if (sessionId != null) {
            activeSessionIds.add(sessionId);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        if (sessionId != null) {
            activeSessionIds.remove(sessionId);
        }
    }
}
