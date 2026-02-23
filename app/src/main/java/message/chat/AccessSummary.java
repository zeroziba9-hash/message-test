package message.chat;

public class AccessSummary {
    private String sender;
    private String channelId;
    private ChatRole role;
    private boolean canRead;
    private boolean canWrite;

    public AccessSummary() {
    }

    public AccessSummary(String sender, String channelId, ChatRole role, boolean canRead, boolean canWrite) {
        this.sender = sender;
        this.channelId = channelId;
        this.role = role;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public ChatRole getRole() {
        return role;
    }

    public void setRole(ChatRole role) {
        this.role = role;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public void setCanRead(boolean canRead) {
        this.canRead = canRead;
    }

    public boolean isCanWrite() {
        return canWrite;
    }

    public void setCanWrite(boolean canWrite) {
        this.canWrite = canWrite;
    }
}
