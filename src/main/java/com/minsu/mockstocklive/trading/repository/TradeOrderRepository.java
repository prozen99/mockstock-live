package com.minsu.mockstocklive.trading.repository;

import com.minsu.mockstocklive.trading.domain.TradeOrder;
import com.minsu.mockstocklive.trading.dto.TradeHistoryItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    long countByUserId(Long userId);

    Page<TradeOrder> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    @Query("""
            select new com.minsu.mockstocklive.trading.dto.TradeHistoryItemResponse(
                trade.id,
                stock.id,
                stock.symbol,
                stock.name,
                trade.tradeType,
                trade.price,
                trade.quantity,
                trade.totalAmount,
                trade.createdAt
            )
            from TradeOrder trade
            join trade.stock stock
            where trade.user.id = :userId
              and (:beforeTradeId is null or trade.id < :beforeTradeId)
            order by trade.id desc
            """)
    List<TradeHistoryItemResponse> findTradeHistorySlice(
            @Param("userId") Long userId,
            @Param("beforeTradeId") Long beforeTradeId,
            Pageable pageable
    );
}
