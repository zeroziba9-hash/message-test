package message.chat;

import java.time.Instant;

public class ChatMessage {
    private long id;
    private String channelId;
    private String sender;
    private String content;
    private Instant sentAt;

    public ChatMessage() {
    }

    public ChatMessage(long id, String channelId, String sender, String content, Instant sentAt) {
        this.id = id;
        this.channelId = channelId;
        this.sender = sender;
        this.content = content;
        this.sentAt = sentAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
