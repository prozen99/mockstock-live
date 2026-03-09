package com.minsu.mockstocklive.chat.controller;

import com.minsu.mockstocklive.chat.dto.ChatMessageRequest;
import com.minsu.mockstocklive.chat.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ChatMessageSocketController {

    private final ChatService chatService;

    public ChatMessageSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat/rooms/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, @Valid ChatMessageRequest request) {
        chatService.sendMessage(roomId, request);
    }
}
