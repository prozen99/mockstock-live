package com.minsu.mockstocklive.chat.repository;

import com.minsu.mockstocklive.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findAllByOrderByIdAsc();

    Optional<ChatRoom> findByStockId(Long stockId);

    boolean existsByStockId(Long stockId);
}
