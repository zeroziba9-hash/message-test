CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    sender VARCHAR(50) PRIMARY KEY,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS channels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id VARCHAR(100) NOT NULL UNIQUE,
    sort_order INT NOT NULL DEFAULT 9999
);

CREATE TABLE IF NOT EXISTS channel_permissions (
    channel_id VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    can_read BOOLEAN NOT NULL,
    can_write BOOLEAN NOT NULL,
    PRIMARY KEY (channel_id, role)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id VARCHAR(100) NOT NULL,
    sender VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_channel_id_id ON chat_messages(channel_id, id DESC);
