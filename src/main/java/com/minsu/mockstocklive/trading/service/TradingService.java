package com.minsu.mockstocklive.trading.service;

import com.minsu.mockstocklive.auth.domain.User;
import com.minsu.mockstocklive.auth.repository.UserRepository;
import com.minsu.mockstocklive.exception.BusinessValidationException;
import com.minsu.mockstocklive.exception.ResourceNotFoundException;
import com.minsu.mockstocklive.monitoring.MonitoringMetrics;
import com.minsu.mockstocklive.portfolio.domain.Holding;
import com.minsu.mockstocklive.portfolio.repository.HoldingRepository;
import com.minsu.mockstocklive.stock.domain.Stock;
import com.minsu.mockstocklive.stock.repository.StockRepository;
import com.minsu.mockstocklive.trading.domain.TradeOrder;
import com.minsu.mockstocklive.trading.domain.TradeType;
import com.minsu.mockstocklive.trading.dto.TradeCursorHistoryResponse;
import com.minsu.mockstocklive.trading.dto.TradeHistoryItemResponse;
import com.minsu.mockstocklive.trading.dto.TradeHistoryResponse;
import com.minsu.mockstocklive.trading.dto.TradeRequest;
import com.minsu.mockstocklive.trading.dto.TradeResponse;
import com.minsu.mockstocklive.trading.repository.TradeOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class TradingService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final HoldingRepository holdingRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final MonitoringMetrics monitoringMetrics;

    public TradingService(
            UserRepository userRepository,
            StockRepository stockRepository,
            HoldingRepository holdingRepository,
            TradeOrderRepository tradeOrderRepository,
            MonitoringMetrics monitoringMetrics
    ) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.holdingRepository = holdingRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.monitoringMetrics = monitoringMetrics;
    }

    public TradeResponse buy(TradeRequest request) {
        monitoringMetrics.recordTradeRequest("buy");
        User user = getUser(request.userId());
        Stock stock = getStock(request.stockId());
        BigDecimal totalAmount = stock.getCurrentPrice().multiply(BigDecimal.valueOf(request.quantity()));

        if (user.getCashBalance().compareTo(totalAmount) < 0) {
            monitoringMetrics.recordTradeValidationFailure("buy", "insufficient_cash");
            throw new BusinessValidationException("Insufficient cash balance");
        }

        user.withdrawCash(totalAmount);

        Holding holding = holdingRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                .map(existingHolding -> {
                    existingHolding.buy(request.quantity(), stock.getCurrentPrice());
                    return existingHolding;
                })
                .orElseGet(() -> Holding.create(user, stock, request.quantity(), stock.getCurrentPrice()));

        Holding savedHolding = holdingRepository.save(holding);
        TradeOrder tradeOrder = tradeOrderRepository.save(
                TradeOrder.create(user, stock, TradeType.BUY, stock.getCurrentPrice(), request.quantity())
        );

        return toTradeResponse(tradeOrder, user, savedHolding);
    }

    public TradeResponse sell(TradeRequest request) {
        monitoringMetrics.recordTradeRequest("sell");
        User user = getUser(request.userId());
        Stock stock = getStock(request.stockId());
        Holding holding = holdingRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                .orElseThrow(() -> {
                    monitoringMetrics.recordTradeValidationFailure("sell", "no_holding");
                    return new BusinessValidationException("No holding found for the requested stock");
                });

        try {
            holding.sell(request.quantity());
        } catch (BusinessValidationException exception) {
            monitoringMetrics.recordTradeValidationFailure("sell", "insufficient_quantity");
            throw exception;
        }

        BigDecimal totalAmount = stock.getCurrentPrice().multiply(BigDecimal.valueOf(request.quantity()));
        user.depositCash(totalAmount);

        TradeOrder tradeOrder = tradeOrderRepository.save(
                TradeOrder.create(user, stock, TradeType.SELL, stock.getCurrentPrice(), request.quantity())
        );

        if (holding.isEmpty()) {
            // Phase 3 policy: remove empty holdings so the table stays a snapshot of current positions.
            holdingRepository.delete(holding);

            return new TradeResponse(
                tradeOrder.getId(),
                user.getId(),
                    stock.getId(),
                    stock.getSymbol(),
                    tradeOrder.getTradeType(),
                    tradeOrder.getPrice(),
                    tradeOrder.getQuantity(),
                    tradeOrder.getTotalAmount(),
                    user.getCashBalance(),
                    0L,
                    BigDecimal.ZERO,
                    tradeOrder.getCreatedAt()
            );
        }

        Holding savedHolding = holdingRepository.save(holding);
        return toTradeResponse(tradeOrder, user, savedHolding);
    }

    @Transactional(readOnly = true)
    public TradeHistoryResponse getTradeHistory(Long userId, int page, int size) {
        return monitoringMetrics.recordRead("trade_history_offset", () -> {
            getUser(userId);
            Page<TradeOrder> tradePage = tradeOrderRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(page, size));

            return new TradeHistoryResponse(
                    tradePage.getContent().stream().map(this::toTradeHistoryItem).toList(),
                    tradePage.getNumber(),
                    tradePage.getSize(),
                    tradePage.getTotalElements(),
                    tradePage.getTotalPages()
            );
        });
    }

    @Transactional(readOnly = true)
    public TradeCursorHistoryResponse getTradeHistoryByCursor(Long userId, Long beforeTradeId, int size) {
        return monitoringMetrics.recordRead("trade_history_cursor", () -> {
            getUser(userId);

            List<TradeHistoryItemResponse> tradeItems = tradeOrderRepository.findTradeHistorySlice(
                    userId,
                    beforeTradeId,
                    PageRequest.of(0, size + 1)
            );

            boolean hasNext = tradeItems.size() > size;
            List<TradeHistoryItemResponse> pageItems = hasNext ? tradeItems.subList(0, size) : tradeItems;
            Long nextBeforeTradeId = hasNext && !pageItems.isEmpty()
                    ? pageItems.get(pageItems.size() - 1).tradeOrderId()
                    : null;

            return new TradeCursorHistoryResponse(
                    pageItems,
                    beforeTradeId,
                    size,
                    hasNext,
                    nextBeforeTradeId
            );
        });
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Stock getStock(Long stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + stockId));
    }

    private TradeResponse toTradeResponse(TradeOrder tradeOrder, User user, Holding holding) {
        return new TradeResponse(
                tradeOrder.getId(),
                user.getId(),
                tradeOrder.getStock().getId(),
                tradeOrder.getStock().getSymbol(),
                tradeOrder.getTradeType(),
                tradeOrder.getPrice(),
                tradeOrder.getQuantity(),
                tradeOrder.getTotalAmount(),
                user.getCashBalance(),
                holding.getQuantity(),
                holding.getAvgBuyPrice(),
                tradeOrder.getCreatedAt()
        );
    }

    private TradeHistoryItemResponse toTradeHistoryItem(TradeOrder tradeOrder) {
        return new TradeHistoryItemResponse(
                tradeOrder.getId(),
                tradeOrder.getStock().getId(),
                tradeOrder.getStock().getSymbol(),
                tradeOrder.getStock().getName(),
                tradeOrder.getTradeType(),
                tradeOrder.getPrice(),
                tradeOrder.getQuantity(),
                tradeOrder.getTotalAmount(),
                tradeOrder.getCreatedAt()
        );
    }
}
