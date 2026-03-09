package com.minsu.mockstocklive.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockDetailResponse(
        Long stockId,
        String symbol,
        String name,
        String marketType,
        BigDecimal currentPrice,
        BigDecimal priceChangeRate,
        LocalDateTime updatedAt
) {
}
