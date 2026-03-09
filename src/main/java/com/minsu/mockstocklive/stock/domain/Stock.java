package com.minsu.mockstocklive.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "market_type", nullable = false, length = 30)
    private String marketType;

    @Column(name = "current_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "price_change_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal priceChangeRate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Stock() {
    }

    private Stock(String symbol, String name, String marketType, BigDecimal currentPrice, BigDecimal priceChangeRate) {
        this.symbol = symbol;
        this.name = name;
        this.marketType = marketType;
        this.currentPrice = currentPrice;
        this.priceChangeRate = priceChangeRate;
    }

    public static Stock create(String symbol, String name, String marketType, BigDecimal currentPrice, BigDecimal priceChangeRate) {
        return new Stock(symbol, name, marketType, currentPrice, priceChangeRate);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public String getMarketType() {
        return marketType;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getPriceChangeRate() {
        return priceChangeRate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
