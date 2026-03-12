package com.minsu.mockstocklive.stock.config;

import com.minsu.mockstocklive.stock.domain.Stock;
import com.minsu.mockstocklive.stock.repository.StockRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile({"local", "deploy"})
public class LocalStockDataInitializer implements ApplicationRunner {

    private final StockRepository stockRepository;

    public LocalStockDataInitializer(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (stockRepository.count() > 0) {
            return;
        }

        stockRepository.saveAll(List.of(
                Stock.create("MSL001", "Mock Tech", "KOSPI", new BigDecimal("125000.00"), new BigDecimal("1.2400")),
                Stock.create("MSL002", "Sample Motors", "KOSDAQ", new BigDecimal("84500.00"), new BigDecimal("-0.8500")),
                Stock.create("MSL003", "Demo Bio", "KOSDAQ", new BigDecimal("53200.00"), new BigDecimal("2.1100"))
        ));
    }
}
