package com.minsu.mockstocklive.portfolio.domain;

import com.minsu.mockstocklive.auth.domain.User;
import com.minsu.mockstocklive.exception.BusinessValidationException;
import com.minsu.mockstocklive.stock.domain.Stock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "holdings")
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "avg_buy_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal avgBuyPrice;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    protected Holding() {
    }

    private Holding(User user, Stock stock, long quantity, BigDecimal avgBuyPrice) {
        this.user = user;
        this.stock = stock;
        this.quantity = quantity;
        this.avgBuyPrice = avgBuyPrice;
    }

    public static Holding create(User user, Stock stock, long quantity, BigDecimal avgBuyPrice) {
        return new Holding(user, stock, quantity, avgBuyPrice);
    }

    public void buy(long buyQuantity, BigDecimal buyPrice) {
        BigDecimal totalBuyCost = buyPrice.multiply(BigDecimal.valueOf(buyQuantity));
        BigDecimal currentTotalCost = avgBuyPrice.multiply(BigDecimal.valueOf(quantity));

        quantity += buyQuantity;
        avgBuyPrice = currentTotalCost.add(totalBuyCost)
                .divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    public void sell(long sellQuantity) {
        if (sellQuantity > quantity) {
            throw new BusinessValidationException("Sell quantity exceeds current holding quantity");
        }

        quantity -= sellQuantity;
    }

    public boolean isEmpty() {
        return quantity == 0;
    }

    @PrePersist
    @PreUpdate
    void updateEvaluatedAt() {
        evaluatedAt = LocalDateTime.now();
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

    public long getQuantity() {
        return quantity;
    }

    public BigDecimal getAvgBuyPrice() {
        return avgBuyPrice;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }
}
