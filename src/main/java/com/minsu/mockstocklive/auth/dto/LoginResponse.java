package com.minsu.mockstocklive.auth.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoginResponse(
        Long userId,
        String email,
        String nickname,
        BigDecimal cashBalance,
        String message,
        LocalDateTime loginAt
) {
}
