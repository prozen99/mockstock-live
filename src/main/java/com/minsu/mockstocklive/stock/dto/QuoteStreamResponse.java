package com.minsu.mockstocklive.stock.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuoteStreamResponse(
        LocalDateTime publishedAt,
        List<QuoteResponse> quotes
) {
}
