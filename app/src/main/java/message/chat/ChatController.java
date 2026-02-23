package message.chat;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    public ChatController(SimpMessagingTemplate messagingTemplate, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    // 클라가 /pub/channels/{channelId} 로 보내면 여기로 들어옴
    @MessageMapping("/channels/{channelId}")
    public void send(@DestinationVariable String channelId, ChatSendRequest request) {
        try {
            ChatMessage message = chatService.saveMessage(channelId, request);
            // 구독자에게 /sub/channels/{channelId} 로 뿌림
            messagingTemplate.convertAndSend("/sub/channels/" + channelId, message);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/api/channels")
    public List<String> listChannels() {
        return chatService.listChannels();
    }

    @GetMapping("/api/channels/{channelId}/messages")
    public List<ChatMessage> getMessages(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            return chatService.getRecentMessages(channelId, limit);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
