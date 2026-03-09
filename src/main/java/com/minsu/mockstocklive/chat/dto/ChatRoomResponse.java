package com.minsu.mockstocklive.chat.dto;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long roomId,
        Long stockId,
        String stockSymbol,
        String stockName,
        String roomName,
        Long lastMessageId,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        boolean joined
) {
}
