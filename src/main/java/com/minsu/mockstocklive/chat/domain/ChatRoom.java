package com.minsu.mockstocklive.chat.domain;

import com.minsu.mockstocklive.stock.domain.Stock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "room_name", nullable = false, length = 100)
    private String roomName;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ChatRoom() {
    }

    private ChatRoom(Stock stock, String roomName) {
        this.stock = stock;
        this.roomName = roomName;
    }

    public static ChatRoom create(Stock stock, String roomName) {
        return new ChatRoom(stock, roomName);
    }

    public void updateLastMessage(ChatMessage chatMessage) {
        lastMessageId = chatMessage.getId();
        lastMessageAt = chatMessage.getCreatedAt();
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        if (lastMessageAt == null) {
            lastMessageAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Stock getStock() {
        return stock;
    }

    public String getRoomName() {
        return roomName;
    }

    public Long getLastMessageId() {
        return lastMessageId;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
