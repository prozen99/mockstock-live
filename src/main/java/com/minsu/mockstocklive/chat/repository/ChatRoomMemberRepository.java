package com.minsu.mockstocklive.chat.repository;

import com.minsu.mockstocklive.chat.domain.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    Optional<ChatRoomMember> findByRoomIdAndUserId(Long roomId, Long userId);
}
