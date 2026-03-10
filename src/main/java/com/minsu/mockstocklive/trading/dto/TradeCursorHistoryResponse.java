package com.minsu.mockstocklive.trading.dto;

import java.util.List;

public record TradeCursorHistoryResponse(
        List<TradeHistoryItemResponse> trades,
        Long requestedBeforeTradeId,
        int size,
        boolean hasNext,
        Long nextBeforeTradeId
) {
}
