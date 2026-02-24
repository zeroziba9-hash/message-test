package message.chat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createChannelIfAbsent(String channelId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM channels WHERE channel_id = ?",
                Integer.class,
                channelId);

        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO channels(channel_id) VALUES(?)", channelId);
        }
    }

    public List<String> listChannels() {
        return jdbcTemplate.query(
                "SELECT channel_id FROM channels ORDER BY sort_order ASC, id ASC",
                (rs, rowNum) -> rs.getString("channel_id"));
    }

    public void replaceChannelOrder(List<String> nextOrder) {
        for (int i = 0; i < nextOrder.size(); i++) {
            String channelId = nextOrder.get(i);
            createChannelIfAbsent(channelId);
            jdbcTemplate.update("UPDATE channels SET sort_order = ? WHERE channel_id = ?", i + 1, channelId);
        }
    }

    public ChatMessage saveMessage(String channelId, String sender, String content, Instant sentAt) {
        jdbcTemplate.update(
                "INSERT INTO chat_messages(channel_id, sender, content, sent_at) VALUES(?, ?, ?, ?)",
                channelId,
                sender,
                content,
                Timestamp.from(sentAt));

        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM chat_messages", Long.class);
        return new ChatMessage(id == null ? 0 : id, channelId, sender, content, sentAt);
    }

    public List<ChatMessage> getRecentMessages(String channelId, int limit) {
        List<ChatMessage> rows = jdbcTemplate.query(
                "SELECT id, channel_id, sender, content, sent_at FROM chat_messages WHERE channel_id = ? ORDER BY id DESC LIMIT ?",
                (rs, rowNum) -> new ChatMessage(
                        rs.getLong("id"),
                        rs.getString("channel_id"),
                        rs.getString("sender"),
                        rs.getString("content"),
                        rs.getTimestamp("sent_at").toInstant()),
                channelId,
                limit);
        Collections.reverse(rows);
        return rows;
    }

    public ChatMessage findMessage(String channelId, long messageId) {
        var rows = jdbcTemplate.query(
                "SELECT id, channel_id, sender, content, sent_at FROM chat_messages WHERE channel_id = ? AND id = ?",
                (rs, rowNum) -> new ChatMessage(
                        rs.getLong("id"),
                        rs.getString("channel_id"),
                        rs.getString("sender"),
                        rs.getString("content"),
                        rs.getTimestamp("sent_at").toInstant()),
                channelId,
                messageId);

        return rows.isEmpty() ? null : rows.get(0);
    }

    public ChatMessage updateMessage(String channelId, long messageId, String content) {
        jdbcTemplate.update(
                "UPDATE chat_messages SET content = ? WHERE channel_id = ? AND id = ?",
                content,
                channelId,
                messageId);

        return findMessage(channelId, messageId);
    }

    public int deleteMessage(String channelId, long messageId) {
        return jdbcTemplate.update("DELETE FROM chat_messages WHERE channel_id = ? AND id = ?", channelId, messageId);
    }
}
