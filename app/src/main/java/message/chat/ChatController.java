package message.chat;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final ChannelPresenceService presenceService;
    private final AccessControlService accessControlService;

    public ChatController(
            SimpMessagingTemplate messagingTemplate,
            ChatService chatService,
            ChannelPresenceService presenceService,
            AccessControlService accessControlService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
        this.presenceService = presenceService;
        this.accessControlService = accessControlService;
    }

    // 채널 입장(접속 사용자 목록 관리)
    @MessageMapping("/channels/{channelId}/join")
    public void join(
            @DestinationVariable String channelId,
            ChatSendRequest request,
            @Header("simpSessionId") String sessionId) {
        try {
            var access = accessControlService.getSummary(request.getSender(), channelId);
            if (!access.isCanRead()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 채널을 읽을 권한이 없습니다");
            }

            var snapshot = presenceService.join(channelId, request.getSender(), sessionId);
            messagingTemplate.convertAndSend("/sub/channels/" + channelId + "/presence", snapshot);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // 클라가 /pub/channels/{channelId} 로 보내면 여기로 들어옴
    @MessageMapping("/channels/{channelId}")
    public void send(@DestinationVariable String channelId, ChatSendRequest request) {
        try {
            var access = accessControlService.getSummary(request.getSender(), channelId);
            if (!access.isCanWrite()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 채널에 메시지를 보낼 권한이 없습니다");
            }

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
            @RequestParam String sender,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            var access = accessControlService.getSummary(sender, channelId);
            if (!access.isCanRead()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 채널을 읽을 권한이 없습니다");
            }

            return chatService.getRecentMessages(channelId, limit);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/api/channels/{channelId}/online-users")
    public PresenceUpdate getOnlineUsers(
            @PathVariable String channelId,
            @RequestParam String sender) {
        try {
            var access = accessControlService.getSummary(sender, channelId);
            if (!access.isCanRead()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 채널을 읽을 권한이 없습니다");
            }

            return presenceService.snapshot(channelId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/api/access/{channelId}")
    public AccessSummary getAccess(
            @PathVariable String channelId,
            @RequestParam String sender) {
        try {
            return accessControlService.getSummary(sender, channelId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/api/admin/users/{sender}/role")
    public AccessSummary setUserRole(
            @PathVariable String sender,
            @RequestParam String role) {
        try {
            return accessControlService.setUserRole(sender, role);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/api/admin/channels/{channelId}/permissions")
    public ChannelPermission setChannelPermission(
            @PathVariable String channelId,
            @RequestParam String role,
            @RequestParam boolean canRead,
            @RequestParam boolean canWrite) {
        try {
            return accessControlService.setChannelPermission(channelId, role, canRead, canWrite);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
