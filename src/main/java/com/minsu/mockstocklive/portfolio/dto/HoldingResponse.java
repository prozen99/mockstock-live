package com.minsu.mockstocklive.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HoldingResponse(
        Long holdingId,
        Long stockId,
        String stockSymbol,
        String stockName,
        long quantity,
        BigDecimal avgBuyPrice,
        BigDecimal currentPrice,
        BigDecimal evaluatedAmount,
        BigDecimal profitLoss,
        BigDecimal profitRate,
        LocalDateTime evaluatedAt
) {
}
