package message.chat;

import java.util.List;

public class PresenceUpdate {
    private String channelId;
    private int onlineCount;
    private List<String> users;

    public PresenceUpdate() {
    }

    public PresenceUpdate(String channelId, int onlineCount, List<String> users) {
        this.channelId = channelId;
        this.onlineCount = onlineCount;
        this.users = users;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public int getOnlineCount() {
        return onlineCount;
    }

    public void setOnlineCount(int onlineCount) {
        this.onlineCount = onlineCount;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }
}
