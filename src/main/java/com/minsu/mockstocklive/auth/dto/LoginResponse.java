package com.minsu.mockstocklive.auth.dto;

import java.time.LocalDateTime;

public record LoginResponse(
        Long userId,
        String email,
        String nickname,
        String message,
        LocalDateTime loginAt
) {
}
