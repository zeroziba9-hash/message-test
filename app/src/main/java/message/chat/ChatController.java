package message.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
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

    @MessageMapping("/channels/{channelId}/join")
    public void join(
            @DestinationVariable String channelId,
            @Valid ChatSendRequest request,
            @Header("simpSessionId") String sessionId) {
        var access = accessControlService.getSummary(request.getSender(), channelId);
        if (!access.isCanRead()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 채널을 읽을 권한이 없습니다");
        }

        var snapshot = presenceService.join(channelId, request.getSender(), sessionId);
        messagingTemplate.convertAndSend("/sub/channels/" + channelId + "/presence", snapshot);
    }

    @MessageMapping("/channels/{channelId}")
    public void send(@DestinationVariable String channelId, @Valid ChatSendRequest request) {
        var access = accessControlService.getSummary(request.getSender(), channelId);
        if (!access.isCanWrite()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 채널에 메시지를 보낼 권한이 없습니다");
        }

        ChatMessage message = chatService.saveMessage(channelId, request);
        messagingTemplate.convertAndSend("/sub/channels/" + channelId, message);
    }

    @GetMapping("/api/channels")
    public List<String> listChannels() {
        return chatService.listChannels();
    }

    @GetMapping("/api/channels/{channelId}/messages")
    public List<ChatMessage> getMessages(
            @PathVariable String channelId,
            @RequestParam @NotBlank(message = "sender는 필수입니다.") String sender,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        var access = accessControlService.getSummary(sender, channelId);
        if (!access.isCanRead()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 채널을 읽을 권한이 없습니다");
        }

        return chatService.getRecentMessages(channelId, limit);
    }

    @GetMapping("/api/channels/{channelId}/online-users")
    public PresenceUpdate getOnlineUsers(
            @PathVariable String channelId,
            @RequestParam @NotBlank(message = "sender는 필수입니다.") String sender) {
        var access = accessControlService.getSummary(sender, channelId);
        if (!access.isCanRead()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 채널을 읽을 권한이 없습니다");
        }

        return presenceService.snapshot(channelId);
    }

    @GetMapping("/api/access/{channelId}")
    public AccessSummary getAccess(
            @PathVariable String channelId,
            @RequestParam @NotBlank(message = "sender는 필수입니다.") String sender) {
        return accessControlService.getSummary(sender, channelId);
    }

    @PostMapping("/api/admin/users/{sender}/role")
    public AccessSummary setUserRole(
            @PathVariable String sender,
            @RequestParam @NotBlank(message = "role은 필수입니다.") String role) {
        return accessControlService.setUserRole(sender, role);
    }

    @PostMapping("/api/admin/channels/{channelId}/permissions")
    public ChannelPermission setChannelPermission(
            @PathVariable String channelId,
            @RequestParam @NotBlank(message = "role은 필수입니다.") String role,
            @RequestParam boolean canRead,
            @RequestParam boolean canWrite) {
        return accessControlService.setChannelPermission(channelId, role, canRead, canWrite);
    }
}
