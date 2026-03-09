package com.minsu.mockstocklive.stock.controller;

import com.minsu.mockstocklive.response.ApiResponse;
import com.minsu.mockstocklive.stock.dto.StockDetailResponse;
import com.minsu.mockstocklive.stock.dto.StockSummaryResponse;
import com.minsu.mockstocklive.stock.service.StockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public ApiResponse<List<StockSummaryResponse>> getStocks() {
        return ApiResponse.success(stockService.getStocks());
    }

    @GetMapping("/{stockId}")
    public ApiResponse<StockDetailResponse> getStock(@PathVariable Long stockId) {
        return ApiResponse.success(stockService.getStock(stockId));
    }
}
