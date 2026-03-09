package com.minsu.mockstocklive.trading.dto;

import com.minsu.mockstocklive.trading.domain.TradeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeHistoryItemResponse(
        Long tradeOrderId,
        Long stockId,
        String stockSymbol,
        String stockName,
        TradeType tradeType,
        BigDecimal price,
        long quantity,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
}
