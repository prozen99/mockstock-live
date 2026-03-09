package com.minsu.mockstocklive.chat.dto;

import java.time.LocalDateTime;

public record ChatJoinResponse(
        Long roomId,
        Long userId,
        boolean alreadyJoined,
        LocalDateTime joinedAt
) {
}
