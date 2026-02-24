package message.chat;

import java.time.Instant;
import java.util.List;
import message.common.error.ApiException;
import message.common.error.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    public static final String DEFAULT_CHANNEL = "자유채팅방";
    private static final int MAX_MESSAGES_PER_CHANNEL = 100;

    private final ChatRepository chatRepository;

    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
        createChannel(DEFAULT_CHANNEL);
    }

    public ChatMessage saveMessage(String channelId, ChatSendRequest request) {
        var normalizedChannelId = normalizeChannelId(channelId);
        createChannel(normalizedChannelId);

        var sender = safeTrim(request.getSender());
        var content = safeTrim(request.getContent());

        if (sender == null || sender.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "sender is required");
        }
        if (content == null || content.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "content is required");
        }

        return chatRepository.saveMessage(normalizedChannelId, sender, content, Instant.now());
    }

    public List<ChatMessage> getRecentMessages(String channelId, int limit) {
        var normalizedChannelId = normalizeChannelId(channelId);
        createChannel(normalizedChannelId);

        int safeLimit = Math.max(1, Math.min(limit, MAX_MESSAGES_PER_CHANNEL));
        return chatRepository.getRecentMessages(normalizedChannelId, safeLimit);
    }

    public ChatMessage findMessage(String channelId, long messageId) {
        var normalizedChannelId = normalizeChannelId(channelId);
        var message = chatRepository.findMessage(normalizedChannelId, messageId);
        if (message == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "message not found");
        }
        return message;
    }

    public ChatMessage updateMessage(String channelId, long messageId, String content) {
        var normalizedContent = safeTrim(content);

        if (normalizedContent == null || normalizedContent.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "content is required");
        }

        findMessage(channelId, messageId);
        return chatRepository.updateMessage(normalizeChannelId(channelId), messageId, normalizedContent);
    }

    public void deleteMessage(String channelId, long messageId) {
        var normalizedChannelId = normalizeChannelId(channelId);
        int deleted = chatRepository.deleteMessage(normalizedChannelId, messageId);
        if (deleted == 0) {
            throw new ApiException(ErrorCode.NOT_FOUND, "message not found");
        }
    }

    public List<String> listChannels() {
        return chatRepository.listChannels();
    }

    public String createChannel(String channelId) {
        var normalized = normalizeChannelId(channelId);
        chatRepository.createChannelIfAbsent(normalized);
        return normalized;
    }

    public List<String> reorderChannels(List<String> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "channelIds is required");
        }

        List<String> normalized = channelIds.stream()
                .map(this::normalizeChannelId)
                .distinct()
                .toList();

        if (!normalized.contains(DEFAULT_CHANNEL)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "기본 채널은 제거할 수 없습니다");
        }

        chatRepository.replaceChannelOrder(normalized);
        return listChannels();
    }

    private String normalizeChannelId(String channelId) {
        var normalized = safeTrim(channelId);
        if (normalized == null || normalized.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "channelId is required");
        }
        return normalized;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }
}
