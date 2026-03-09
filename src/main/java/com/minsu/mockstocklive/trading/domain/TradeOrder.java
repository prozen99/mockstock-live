package com.minsu.mockstocklive.trading.domain;

import com.minsu.mockstocklive.auth.domain.User;
import com.minsu.mockstocklive.stock.domain.Stock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_orders")
public class TradeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, length = 20)
    private TradeType tradeType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected TradeOrder() {
    }

    private TradeOrder(User user, Stock stock, TradeType tradeType, BigDecimal price, long quantity, BigDecimal totalAmount) {
        this.user = user;
        this.stock = stock;
        this.tradeType = tradeType;
        this.price = price;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }

    public static TradeOrder create(User user, Stock stock, TradeType tradeType, BigDecimal price, long quantity) {
        return new TradeOrder(
                user,
                stock,
                tradeType,
                price,
                quantity,
                price.multiply(BigDecimal.valueOf(quantity))
        );
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Stock getStock() {
        return stock;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
