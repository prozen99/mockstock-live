package com.minsu.mockstocklive.portfolio.repository;

import com.minsu.mockstocklive.portfolio.domain.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    List<Holding> findByUserIdOrderByIdAsc(Long userId);

    Optional<Holding> findByUserIdAndStockId(Long userId, Long stockId);
}
