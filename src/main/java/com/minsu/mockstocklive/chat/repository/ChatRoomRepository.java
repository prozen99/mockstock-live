package com.minsu.mockstocklive.chat.repository;

import com.minsu.mockstocklive.chat.domain.ChatRoom;
import com.minsu.mockstocklive.chat.dto.ChatRoomResponse;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findAllByOrderByIdAsc();

    @Query("""
            select new com.minsu.mockstocklive.chat.dto.ChatRoomResponse(
                room.id,
                stock.id,
                stock.symbol,
                stock.name,
                room.roomName,
                room.lastMessageId,
                lastMessage.content,
                room.lastMessageAt,
                false
            )
            from ChatRoom room
            join room.stock stock
            left join ChatMessage lastMessage on lastMessage.id = room.lastMessageId
            order by room.id asc
            """)
    List<ChatRoomResponse> findRoomResponses();

    @Query("""
            select new com.minsu.mockstocklive.chat.dto.ChatRoomResponse(
                room.id,
                stock.id,
                stock.symbol,
                stock.name,
                room.roomName,
                room.lastMessageId,
                lastMessage.content,
                room.lastMessageAt,
                case when member.id is not null then true else false end
            )
            from ChatRoom room
            join room.stock stock
            left join ChatMessage lastMessage on lastMessage.id = room.lastMessageId
            left join ChatRoomMember member on member.room = room and member.user.id = :userId
            order by room.id asc
            """)
    List<ChatRoomResponse> findRoomResponses(@Param("userId") Long userId);

    Optional<ChatRoom> findByStockId(Long stockId);

    boolean existsByStockId(Long stockId);
}
