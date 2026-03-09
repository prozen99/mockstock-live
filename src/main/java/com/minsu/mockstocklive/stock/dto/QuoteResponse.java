package com.minsu.mockstocklive.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuoteResponse(
        Long stockId,
        String symbol,
        BigDecimal currentPrice,
        BigDecimal priceChangeRate,
        LocalDateTime updatedAt
) {
}
