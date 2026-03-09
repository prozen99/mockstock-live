package com.minsu.mockstocklive.chat.controller;

import com.minsu.mockstocklive.chat.dto.ChatJoinRequest;
import com.minsu.mockstocklive.chat.dto.ChatJoinResponse;
import com.minsu.mockstocklive.chat.dto.ChatMessagePageResponse;
import com.minsu.mockstocklive.chat.dto.ChatRoomResponse;
import com.minsu.mockstocklive.chat.service.ChatService;
import com.minsu.mockstocklive.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/chat/rooms")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public ApiResponse<List<ChatRoomResponse>> getRooms(@RequestParam(required = false) Long userId) {
        return ApiResponse.success(chatService.getRooms(userId));
    }

    @GetMapping("/{roomId}/messages")
    public ApiResponse<ChatMessagePageResponse> getMessages(
            @PathVariable @Positive Long roomId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "30") @Positive @Max(100) int size
    ) {
        return ApiResponse.success(chatService.getMessages(roomId, page, size));
    }

    @PostMapping("/{roomId}/join")
    public ApiResponse<ChatJoinResponse> joinRoom(
            @PathVariable @Positive Long roomId,
            @Valid @RequestBody ChatJoinRequest request
    ) {
        return ApiResponse.success(chatService.joinRoom(roomId, request.userId()));
    }
}
