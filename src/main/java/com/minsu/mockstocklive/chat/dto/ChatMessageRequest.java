package com.minsu.mockstocklive.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @Positive
        Long userId,

        @NotBlank
        @Size(max = 1000)
        String content
) {
}
