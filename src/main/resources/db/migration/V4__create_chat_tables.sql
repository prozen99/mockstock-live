CREATE TABLE chat_rooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_id BIGINT NOT NULL,
    room_name VARCHAR(100) NOT NULL,
    last_message_id BIGINT NULL,
    last_message_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_chat_rooms PRIMARY KEY (id),
    CONSTRAINT uk_chat_rooms_stock UNIQUE (stock_id),
    CONSTRAINT fk_chat_rooms_stock FOREIGN KEY (stock_id) REFERENCES stocks (id)
);

CREATE INDEX idx_chat_rooms_last_message_at ON chat_rooms (last_message_at);

CREATE TABLE chat_room_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_message_id BIGINT NULL,
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_chat_room_members PRIMARY KEY (id),
    CONSTRAINT uk_chat_room_members_room_user UNIQUE (room_id, user_id),
    CONSTRAINT fk_chat_room_members_room FOREIGN KEY (room_id) REFERENCES chat_rooms (id),
    CONSTRAINT fk_chat_room_members_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_chat_room_members_user_room ON chat_room_members (user_id, room_id);

CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted BIT(1) NOT NULL DEFAULT b'0',
    CONSTRAINT pk_chat_messages PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_room FOREIGN KEY (room_id) REFERENCES chat_rooms (id),
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)
);

CREATE INDEX idx_chat_messages_room_id_id ON chat_messages (room_id, id);
