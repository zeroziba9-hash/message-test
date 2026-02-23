package message.chat;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventHandler {

    private final ChannelPresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventHandler(ChannelPresenceService presenceService, SimpMessagingTemplate messagingTemplate) {
        this.presenceService = presenceService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        if (event.getSessionId() == null || event.getSessionId().isBlank()) {
            return;
        }

        var previous = presenceService.leaveBySession(event.getSessionId());
        if (previous == null) {
            return;
        }

        var snapshot = presenceService.snapshot(previous.channelId());
        messagingTemplate.convertAndSend("/sub/channels/" + previous.channelId() + "/presence", snapshot);
    }
}
