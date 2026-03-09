package com.minsu.mockstocklive.stock.repository;

import com.minsu.mockstocklive.stock.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, Long> {

    List<Stock> findAllByOrderBySymbolAsc();
}
