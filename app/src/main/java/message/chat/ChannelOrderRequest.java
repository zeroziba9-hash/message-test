package message.chat;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ChannelOrderRequest {

    @NotEmpty(message = "channelIds는 필수입니다.")
    private List<String> channelIds;

    public List<String> getChannelIds() {
        return channelIds;
    }

    public void setChannelIds(List<String> channelIds) {
        this.channelIds = channelIds;
    }
}
