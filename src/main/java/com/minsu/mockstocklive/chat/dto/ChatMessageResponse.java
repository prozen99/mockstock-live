package com.minsu.mockstocklive.chat.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long roomId,
        Long senderId,
        String senderNickname,
        String content,
        boolean deleted,
        LocalDateTime createdAt
) {
}
