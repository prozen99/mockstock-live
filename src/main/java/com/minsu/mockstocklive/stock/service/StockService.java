package com.minsu.mockstocklive.stock.service;

import com.minsu.mockstocklive.exception.ResourceNotFoundException;
import com.minsu.mockstocklive.stock.domain.Stock;
import com.minsu.mockstocklive.stock.dto.StockDetailResponse;
import com.minsu.mockstocklive.stock.dto.StockSummaryResponse;
import com.minsu.mockstocklive.stock.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<StockSummaryResponse> getStocks() {
        return stockRepository.findAllByOrderBySymbolAsc().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public StockDetailResponse getStock(Long stockId) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + stockId));

        return toDetailResponse(stock);
    }

    private StockSummaryResponse toSummaryResponse(Stock stock) {
        return new StockSummaryResponse(
                stock.getId(),
                stock.getSymbol(),
                stock.getName(),
                stock.getMarketType(),
                stock.getCurrentPrice(),
                stock.getPriceChangeRate(),
                stock.getUpdatedAt()
        );
    }

    private StockDetailResponse toDetailResponse(Stock stock) {
        return new StockDetailResponse(
                stock.getId(),
                stock.getSymbol(),
                stock.getName(),
                stock.getMarketType(),
                stock.getCurrentPrice(),
                stock.getPriceChangeRate(),
                stock.getUpdatedAt()
        );
    }
}
