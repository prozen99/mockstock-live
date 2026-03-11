package com.minsu.mockstocklive.auth.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SignupResponse(
        Long userId,
        String email,
        String nickname,
        BigDecimal cashBalance,
        LocalDateTime createdAt
) {
}
