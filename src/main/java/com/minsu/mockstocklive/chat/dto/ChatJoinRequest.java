package com.minsu.mockstocklive.chat.dto;

import jakarta.validation.constraints.Positive;

public record ChatJoinRequest(
        @Positive
        Long userId
) {
}
