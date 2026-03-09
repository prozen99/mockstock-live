package com.minsu.mockstocklive.chat.repository;

import com.minsu.mockstocklive.chat.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByRoomIdOrderByIdDesc(Long roomId, Pageable pageable);
}
