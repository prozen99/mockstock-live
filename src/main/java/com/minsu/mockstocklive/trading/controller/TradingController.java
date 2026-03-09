package com.minsu.mockstocklive.trading.controller;

import com.minsu.mockstocklive.response.ApiResponse;
import com.minsu.mockstocklive.trading.dto.TradeHistoryResponse;
import com.minsu.mockstocklive.trading.dto.TradeRequest;
import com.minsu.mockstocklive.trading.dto.TradeResponse;
import com.minsu.mockstocklive.trading.service.TradingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/trades")
public class TradingController {

    private final TradingService tradingService;

    public TradingController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @PostMapping("/buy")
    public ApiResponse<TradeResponse> buy(@Valid @RequestBody TradeRequest request) {
        return ApiResponse.success(tradingService.buy(request));
    }

    @PostMapping("/sell")
    public ApiResponse<TradeResponse> sell(@Valid @RequestBody TradeRequest request) {
        return ApiResponse.success(tradingService.sell(request));
    }

    @GetMapping("/history")
    public ApiResponse<TradeHistoryResponse> getTradeHistory(
            @RequestParam @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(100) int size
    ) {
        return ApiResponse.success(tradingService.getTradeHistory(userId, page, size));
    }
}
