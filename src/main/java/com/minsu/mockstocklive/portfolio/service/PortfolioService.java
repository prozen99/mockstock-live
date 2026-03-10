package com.minsu.mockstocklive.portfolio.service;

import com.minsu.mockstocklive.monitoring.MonitoringMetrics;
import com.minsu.mockstocklive.portfolio.domain.Holding;
import com.minsu.mockstocklive.portfolio.dto.HoldingResponse;
import com.minsu.mockstocklive.portfolio.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final MonitoringMetrics monitoringMetrics;

    public PortfolioService(HoldingRepository holdingRepository, MonitoringMetrics monitoringMetrics) {
        this.holdingRepository = holdingRepository;
        this.monitoringMetrics = monitoringMetrics;
    }

    public List<HoldingResponse> getHoldings(Long userId) {
        return monitoringMetrics.recordRead("holdings_list", () -> holdingRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(this::toResponse)
                .toList());
    }

    private HoldingResponse toResponse(Holding holding) {
        BigDecimal quantity = BigDecimal.valueOf(holding.getQuantity());
        BigDecimal evaluatedAmount = holding.getStock().getCurrentPrice().multiply(quantity);
        BigDecimal buyAmount = holding.getAvgBuyPrice().multiply(quantity);
        BigDecimal profitLoss = evaluatedAmount.subtract(buyAmount);
        BigDecimal profitRate = buyAmount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profitLoss.multiply(BigDecimal.valueOf(100))
                .divide(buyAmount, 2, RoundingMode.HALF_UP);

        return new HoldingResponse(
                holding.getId(),
                holding.getStock().getId(),
                holding.getStock().getSymbol(),
                holding.getStock().getName(),
                holding.getQuantity(),
                holding.getAvgBuyPrice(),
                holding.getStock().getCurrentPrice(),
                evaluatedAmount,
                profitLoss,
                profitRate,
                holding.getEvaluatedAt()
        );
    }
}
