package message.chat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private static final int MAX_MESSAGES_PER_CHANNEL = 100;

    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<String, Deque<ChatMessage>> channelMessages = new ConcurrentHashMap<>();

    public ChatMessage saveMessage(String channelId, ChatSendRequest request) {
        var normalizedChannelId = normalizeChannelId(channelId);
        var sender = safeTrim(request.getSender());
        var content = safeTrim(request.getContent());

        if (sender == null || sender.isBlank()) {
            throw new IllegalArgumentException("sender is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }

        ChatMessage message = new ChatMessage(
                sequence.getAndIncrement(),
                normalizedChannelId,
                sender,
                content,
                Instant.now());

        channelMessages.compute(normalizedChannelId, (key, deque) -> {
            Deque<ChatMessage> messages = (deque != null) ? deque : new ConcurrentLinkedDeque<>();
            messages.addLast(message);
            while (messages.size() > MAX_MESSAGES_PER_CHANNEL) {
                messages.pollFirst();
            }
            return messages;
        });

        return message;
    }

    public List<ChatMessage> getRecentMessages(String channelId, int limit) {
        var normalizedChannelId = normalizeChannelId(channelId);
        var messages = channelMessages.getOrDefault(normalizedChannelId, new ConcurrentLinkedDeque<>());

        int safeLimit = Math.max(1, Math.min(limit, MAX_MESSAGES_PER_CHANNEL));
        List<ChatMessage> all = new ArrayList<>(messages);
        int fromIndex = Math.max(0, all.size() - safeLimit);
        return all.subList(fromIndex, all.size());
    }

    public List<String> listChannels() {
        return new ArrayList<>(channelMessages.keySet());
    }

    private String normalizeChannelId(String channelId) {
        var normalized = safeTrim(channelId);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("channelId is required");
        }
        return normalized;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }
}
