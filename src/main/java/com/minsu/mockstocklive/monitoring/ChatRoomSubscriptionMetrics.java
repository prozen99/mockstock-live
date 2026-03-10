package com.minsu.mockstocklive.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ChatRoomSubscriptionMetrics {

    private static final String CHAT_DESTINATION_PREFIX = "/sub/chat/rooms/";

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicInteger> roomSubscriptionCounts = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> sessionSubscriptions = new ConcurrentHashMap<>();

    public ChatRoomSubscriptionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        subscribe(accessor.getSessionId(), accessor.getSubscriptionId(), accessor.getDestination());
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        unsubscribe(accessor.getSessionId(), accessor.getSubscriptionId());
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        disconnect(StompHeaderAccessor.wrap(event.getMessage()).getSessionId());
    }

    public void subscribe(String sessionId, String subscriptionId, String destination) {
        if (sessionId == null || subscriptionId == null) {
            return;
        }

        String roomId = extractRoomId(destination);
        if (roomId == null) {
            return;
        }

        Map<String, String> subscriptions = sessionSubscriptions.computeIfAbsent(
                sessionId,
                ignored -> new ConcurrentHashMap<>()
        );

        String previousRoomId = subscriptions.putIfAbsent(subscriptionId, roomId);
        if (previousRoomId != null) {
            return;
        }

        roomCounter(roomId).incrementAndGet();
    }

    public void unsubscribe(String sessionId, String subscriptionId) {
        if (sessionId == null || subscriptionId == null) {
            return;
        }

        Map<String, String> subscriptions = sessionSubscriptions.get(sessionId);
        if (subscriptions == null) {
            return;
        }

        String roomId = subscriptions.remove(subscriptionId);
        if (roomId != null) {
            decrementRoom(roomId);
        }

        if (subscriptions.isEmpty()) {
            sessionSubscriptions.remove(sessionId);
        }
    }

    public void disconnect(String sessionId) {
        if (sessionId == null) {
            return;
        }

        Map<String, String> subscriptions = sessionSubscriptions.remove(sessionId);
        if (subscriptions == null) {
            return;
        }

        subscriptions.values().forEach(this::decrementRoom);
    }

    private AtomicInteger roomCounter(String roomId) {
        return roomSubscriptionCounts.computeIfAbsent(roomId, ignored -> {
            AtomicInteger counter = new AtomicInteger();
            Gauge.builder("mockstock.chat.room.subscriptions.active", counter, AtomicInteger::get)
                    .description("Current active chat subscriptions per room")
                    .tag("roomId", roomId)
                    .register(meterRegistry);
            return counter;
        });
    }

    private void decrementRoom(String roomId) {
        AtomicInteger counter = roomSubscriptionCounts.get(roomId);
        if (counter == null) {
            return;
        }

        counter.updateAndGet(current -> Math.max(0, current - 1));
    }

    private String extractRoomId(String destination) {
        if (destination == null || !destination.startsWith(CHAT_DESTINATION_PREFIX)) {
            return null;
        }

        return destination.substring(CHAT_DESTINATION_PREFIX.length());
    }
}
