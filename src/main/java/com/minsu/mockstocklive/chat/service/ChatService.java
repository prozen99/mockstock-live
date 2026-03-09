package com.minsu.mockstocklive.chat.service;

import com.minsu.mockstocklive.auth.domain.User;
import com.minsu.mockstocklive.auth.repository.UserRepository;
import com.minsu.mockstocklive.chat.domain.ChatMessage;
import com.minsu.mockstocklive.chat.domain.ChatRoom;
import com.minsu.mockstocklive.chat.domain.ChatRoomMember;
import com.minsu.mockstocklive.chat.dto.ChatJoinResponse;
import com.minsu.mockstocklive.chat.dto.ChatMessagePageResponse;
import com.minsu.mockstocklive.chat.dto.ChatMessageRequest;
import com.minsu.mockstocklive.chat.dto.ChatMessageResponse;
import com.minsu.mockstocklive.chat.dto.ChatRoomResponse;
import com.minsu.mockstocklive.chat.repository.ChatMessageRepository;
import com.minsu.mockstocklive.chat.repository.ChatRoomMemberRepository;
import com.minsu.mockstocklive.chat.repository.ChatRoomRepository;
import com.minsu.mockstocklive.exception.BusinessValidationException;
import com.minsu.mockstocklive.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(
            ChatRoomRepository chatRoomRepository,
            ChatRoomMemberRepository chatRoomMemberRepository,
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRooms(Long userId) {
        return chatRoomRepository.findAllByOrderByIdAsc().stream()
                .map(room -> toRoomResponse(room, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatMessagePageResponse getMessages(Long roomId, int page, int size) {
        getRoom(roomId);
        Page<ChatMessage> messagePage = chatMessageRepository.findByRoomIdOrderByIdDesc(roomId, PageRequest.of(page, size));

        return new ChatMessagePageResponse(
                messagePage.getContent().stream().map(this::toMessageResponse).toList(),
                messagePage.getNumber(),
                messagePage.getSize(),
                messagePage.getTotalElements(),
                messagePage.getTotalPages()
        );
    }

    public ChatJoinResponse joinRoom(Long roomId, Long userId) {
        ChatRoom room = getRoom(roomId);
        User user = getUser(userId);

        return chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .map(member -> new ChatJoinResponse(roomId, userId, true, member.getJoinedAt()))
                .orElseGet(() -> {
                    ChatRoomMember savedMember = chatRoomMemberRepository.save(ChatRoomMember.create(room, user));
                    return new ChatJoinResponse(roomId, userId, false, savedMember.getJoinedAt());
                });
    }

    public void sendMessage(Long roomId, ChatMessageRequest request) {
        ChatRoom room = getRoom(roomId);
        User user = getUser(request.userId());

        if (!chatRoomMemberRepository.existsByRoomIdAndUserId(roomId, user.getId())) {
            throw new BusinessValidationException("Join the room before sending messages");
        }

        ChatMessage savedMessage = chatMessageRepository.save(ChatMessage.create(room, user, request.content().trim()));
        room.updateLastMessage(savedMessage);
        chatRoomRepository.save(room);

        messagingTemplate.convertAndSend("/sub/chat/rooms/" + roomId, toMessageResponse(savedMessage));
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room, Long userId) {
        String lastMessagePreview = room.getLastMessageId() == null
                ? null
                : chatMessageRepository.findById(room.getLastMessageId())
                .map(ChatMessage::getContent)
                .orElse(null);

        boolean joined = userId != null && chatRoomMemberRepository.existsByRoomIdAndUserId(room.getId(), userId);

        return new ChatRoomResponse(
                room.getId(),
                room.getStock().getId(),
                room.getStock().getSymbol(),
                room.getStock().getName(),
                room.getRoomName(),
                room.getLastMessageId(),
                lastMessagePreview,
                room.getLastMessageAt(),
                joined
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage chatMessage) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getRoom().getId(),
                chatMessage.getSender().getId(),
                chatMessage.getSender().getNickname(),
                chatMessage.getContent(),
                chatMessage.isDeleted(),
                chatMessage.getCreatedAt()
        );
    }

    private ChatRoom getRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + roomId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
