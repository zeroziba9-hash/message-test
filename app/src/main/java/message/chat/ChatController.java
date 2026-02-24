package message.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import message.common.api.ApiResponse;
import message.common.error.ApiException;
import message.common.error.ErrorCode;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            throw new ApiException(ErrorCode.FORBIDDEN, "이 채널을 읽을 권한이 없습니다");
        }

        var snapshot = presenceService.join(channelId, request.getSender(), sessionId);
        messagingTemplate.convertAndSend("/sub/channels/" + channelId + "/presence", snapshot);
    }

    @MessageMapping("/channels/{channelId}")
    public void send(@DestinationVariable String channelId, @Valid ChatSendRequest request) {
        var access = accessControlService.getSummary(request.getSender(), channelId);
        if (!access.isCanWrite()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이 채널에 메시지를 보낼 권한이 없습니다");
        }

        ChatMessage message = chatService.saveMessage(channelId, request);
        messagingTemplate.convertAndSend("/sub/channels/" + channelId, message);
    }

    @GetMapping("/api/channels")
    public ApiResponse<List<String>> listChannels() {
        return ApiResponse.ok(chatService.listChannels());
    }

    @PostMapping("/api/admin/channels")
    public ApiResponse<List<String>> createChannel(
            @RequestParam @NotBlank(message = "sender는 필수입니다.") String sender,
            @RequestParam @NotBlank(message = "channelId는 필수입니다.") String channelId) {
        requireAdmin(sender, ChatService.DEFAULT_CHANNEL);
        chatService.createChannel(channelId);
        return ApiResponse.ok(chatService.listChannels());
    }

    @PostMapping("/api/admin/channels/reorder")
    public ApiResponse<List<String>> reorderChannels(
            @RequestParam @NotBlank(message = "sender는 필수입니다.") String sender,
            @Valid @RequestBody ChannelOrderRequest request) {
        requireAdmin(sender, ChatService.DEFAULT_CHANNEL);
        return ApiResponse.ok(chatService.reorderChannels(request.getChannelIds()));
    }

    @GetMapping("/api/channels/{channelId}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(
            @PathVariable String channelId,
            @RequestParam @NotBlank(message = "sender는 필수입니다.") String sender,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        var access = accessControlService.getSummary(sender, channelId);
        if (!access.isCanRead()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이 채널을 읽을 권한이 없습니다");
        }

        return ApiResponse.ok(chatService.getRecentMessages(channelId, limit));
    }

    @GetMapping("/api/channels/{channelId}/online-users")
    public ApiResponse<PresenceUpdate> getOnlineUsers(
            @PathVariable String channelId,
            @RequestParam @NotBlank(message = "sender는 필수입니다.") String sender) {
        var access = accessControlService.getSummary(sender, channelId);
        if (!access.isCanRead()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이 채널을 읽을 권한이 없습니다");
        }

        return ApiResponse.ok(presenceService.snapshot(channelId));
    }

    @GetMapping("/api/access/{channelId}")
    public ApiResponse<AccessSummary> getAccess(
            @PathVariable String channelId,
            @RequestParam @NotBlank(message = "sender는 필수입니다.") String sender) {
        return ApiResponse.ok(accessControlService.getSummary(sender, channelId));
    }

    @PostMapping("/api/admin/users/{sender}/role")
    public ApiResponse<AccessSummary> setUserRole(
            @PathVariable String sender,
            @RequestParam @NotBlank(message = "actor는 필수입니다.") String actor,
            @RequestParam @NotBlank(message = "role은 필수입니다.") String role) {
        requireAdmin(actor, ChatService.DEFAULT_CHANNEL);
        return ApiResponse.ok(accessControlService.setUserRole(sender, role));
    }

    @PostMapping("/api/admin/channels/{channelId}/permissions")
    public ApiResponse<ChannelPermission> setChannelPermission(
            @PathVariable String channelId,
            @RequestParam @NotBlank(message = "sender는 필수입니다.") String sender,
            @RequestParam @NotBlank(message = "role은 필수입니다.") String role,
            @RequestParam boolean canRead,
            @RequestParam boolean canWrite) {
        requireAdmin(sender, channelId);
        return ApiResponse.ok(accessControlService.setChannelPermission(channelId, role, canRead, canWrite));
    }

    @PutMapping("/api/channels/{channelId}/messages/{messageId}")
    public ApiResponse<ChatMessage> updateMessage(
            @PathVariable String channelId,
            @PathVariable long messageId,
            @RequestParam @NotBlank(message = "sender는 필수입니다.") String sender,
            @Valid @RequestBody MessageUpdateRequest request) {
        ChatMessage target = chatService.findMessage(channelId, messageId);
        requireMessagePermission(sender, channelId, target);

        ChatMessage updated = chatService.updateMessage(channelId, messageId, request.getContent());
        messagingTemplate.convertAndSend("/sub/channels/" + channelId, updated);
        return ApiResponse.ok(updated);
    }

    @DeleteMapping("/api/channels/{channelId}/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(
            @PathVariable String channelId,
            @PathVariable long messageId,
            @RequestParam @NotBlank(message = "sender는 필수입니다.") String sender) {
        ChatMessage target = chatService.findMessage(channelId, messageId);
        requireMessagePermission(sender, channelId, target);

        chatService.deleteMessage(channelId, messageId);
        String actor = sender.equals(target.getSender()) ? "사용자" : "관리자";
        messagingTemplate.convertAndSend(
                "/sub/channels/" + channelId,
                new ChatMessage(messageId, channelId, "시스템", "[deleted]" + actor, Instant.now()));
        return ApiResponse.ok(null);
    }

    private void requireAdmin(String sender, String channelId) {
        var access = accessControlService.getSummary(sender, channelId);
        if (access.getRole() != ChatRole.ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN, "관리자만 수행할 수 있습니다");
        }
    }

    private void requireMessagePermission(String sender, String channelId, ChatMessage target) {
        var access = accessControlService.getSummary(sender, channelId);
        boolean isAdmin = access.getRole() == ChatRole.ADMIN;
        boolean isOwner = sender.equals(target.getSender());

        if (!isAdmin && !isOwner) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인 메시지 또는 관리자만 수정/삭제할 수 있습니다");
        }
    }
}

