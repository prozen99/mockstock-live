package com.minsu.mockstocklive.portfolio.controller;

import com.minsu.mockstocklive.portfolio.dto.HoldingResponse;
import com.minsu.mockstocklive.portfolio.service.PortfolioService;
import com.minsu.mockstocklive.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/holdings")
    public ApiResponse<List<HoldingResponse>> getHoldings(@RequestParam @Positive Long userId) {
        return ApiResponse.success(portfolioService.getHoldings(userId));
    }
}
