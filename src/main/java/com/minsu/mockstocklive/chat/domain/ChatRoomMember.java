package com.minsu.mockstocklive.chat.domain;

import com.minsu.mockstocklive.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room_members")
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    protected ChatRoomMember() {
    }

    private ChatRoomMember(ChatRoom room, User user) {
        this.room = room;
        this.user = user;
    }

    public static ChatRoomMember create(ChatRoom room, User user) {
        return new ChatRoomMember(room, user);
    }

    @PrePersist
    void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ChatRoom getRoom() {
        return room;
    }

    public User getUser() {
        return user;
    }

    public Long getLastReadMessageId() {
        return lastReadMessageId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
