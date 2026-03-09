package com.minsu.mockstocklive.trading.dto;

import jakarta.validation.constraints.Positive;

public record TradeRequest(
        @Positive
        Long userId,

        @Positive
        Long stockId,

        @Positive
        long quantity
) {
}
