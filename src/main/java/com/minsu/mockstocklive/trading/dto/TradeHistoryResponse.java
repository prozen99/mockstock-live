package com.minsu.mockstocklive.trading.dto;

import java.util.List;

public record TradeHistoryResponse(
        List<TradeHistoryItemResponse> trades,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
