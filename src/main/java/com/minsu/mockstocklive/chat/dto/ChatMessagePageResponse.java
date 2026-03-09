package com.minsu.mockstocklive.chat.dto;

import java.util.List;

public record ChatMessagePageResponse(
        List<ChatMessageResponse> messages,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
