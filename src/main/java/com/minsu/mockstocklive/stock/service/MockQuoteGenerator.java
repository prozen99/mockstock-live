package com.minsu.mockstocklive.stock.service;

import com.minsu.mockstocklive.stock.domain.Stock;
import com.minsu.mockstocklive.stock.repository.StockRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("local")
public class MockQuoteGenerator {

    private static final int[] BASIS_POINT_PATTERN = {12, -7, 5, -9, 8, -4};
    private static final BigDecimal MINIMUM_PRICE = new BigDecimal("1000.00");
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    private final StockRepository stockRepository;
    private final QuoteStreamService quoteStreamService;
    private final AtomicLong cycle = new AtomicLong();

    public MockQuoteGenerator(StockRepository stockRepository, QuoteStreamService quoteStreamService) {
        this.stockRepository = stockRepository;
        this.quoteStreamService = quoteStreamService;
    }

    @Scheduled(initialDelay = 2000, fixedDelay = 3000)
    @Transactional
    public void updateQuotes() {
        List<Stock> stocks = stockRepository.findAllByOrderBySymbolAsc();
        if (stocks.isEmpty()) {
            return;
        }

        long currentCycle = cycle.getAndIncrement();
        for (int index = 0; index < stocks.size(); index++) {
            Stock stock = stocks.get(index);
            stock.updateQuote(calculateNextPrice(stock.getCurrentPrice(), currentCycle, index));
        }

        stockRepository.flush();
        quoteStreamService.publishQuotes(stocks);
    }

    private BigDecimal calculateNextPrice(BigDecimal currentPrice, long cycle, int stockIndex) {
        int basisPoints = BASIS_POINT_PATTERN[(int) ((cycle + stockIndex) % BASIS_POINT_PATTERN.length)];
        BigDecimal ratio = BigDecimal.valueOf(10000L + basisPoints)
                .divide(BigDecimal.valueOf(10000), 6, RoundingMode.HALF_UP);

        BigDecimal nextPrice = currentPrice.multiply(ratio)
                .setScale(2, RoundingMode.HALF_UP);

        if (nextPrice.compareTo(currentPrice) == 0) {
            nextPrice = basisPoints >= 0 ? currentPrice.add(ONE_CENT) : currentPrice.subtract(ONE_CENT);
        }

        return nextPrice.max(MINIMUM_PRICE);
    }
}
