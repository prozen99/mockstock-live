package com.minsu.mockstocklive.trading.repository;

import com.minsu.mockstocklive.trading.domain.TradeOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    Page<TradeOrder> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
