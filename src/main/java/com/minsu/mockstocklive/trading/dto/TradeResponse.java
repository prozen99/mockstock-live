package com.minsu.mockstocklive.trading.dto;

import com.minsu.mockstocklive.trading.domain.TradeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeResponse(
        Long tradeOrderId,
        Long userId,
        Long stockId,
        String stockSymbol,
        TradeType tradeType,
        BigDecimal executedPrice,
        long quantity,
        BigDecimal totalAmount,
        BigDecimal remainingCashBalance,
        long holdingQuantity,
        BigDecimal holdingAvgBuyPrice,
        LocalDateTime tradedAt
) {
}
