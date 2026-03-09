package com.minsu.mockstocklive.auth.dto;

import java.time.LocalDateTime;

public record SignupResponse(
        Long userId,
        String email,
        String nickname,
        LocalDateTime createdAt
) {
}
