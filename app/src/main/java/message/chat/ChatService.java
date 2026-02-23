package message.chat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    public static final String DEFAULT_CHANNEL = "자유채팅방";
    private static final int MAX_MESSAGES_PER_CHANNEL = 100;

    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<String, Deque<ChatMessage>> channelMessages = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String> channelOrder = new CopyOnWriteArrayList<>();

    public ChatService() {
        createChannel(DEFAULT_CHANNEL);
    }

    public ChatMessage saveMessage(String channelId, ChatSendRequest request) {
        var normalizedChannelId = normalizeChannelId(channelId);
        createChannel(normalizedChannelId);

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
        createChannel(normalizedChannelId);

        var messages = channelMessages.getOrDefault(normalizedChannelId, new ConcurrentLinkedDeque<>());
        int safeLimit = Math.max(1, Math.min(limit, MAX_MESSAGES_PER_CHANNEL));
        List<ChatMessage> all = new ArrayList<>(messages);
        int fromIndex = Math.max(0, all.size() - safeLimit);
        return all.subList(fromIndex, all.size());
    }

    public ChatMessage updateMessage(String channelId, long messageId, String content) {
        var normalizedChannelId = normalizeChannelId(channelId);
        var normalizedContent = safeTrim(content);

        if (normalizedContent == null || normalizedContent.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }

        var messages = channelMessages.getOrDefault(normalizedChannelId, new ConcurrentLinkedDeque<>());
        for (ChatMessage message : messages) {
            if (message.getId() == messageId) {
                message.setContent(normalizedContent);
                return message;
            }
        }

        throw new IllegalArgumentException("message not found");
    }

    public void deleteMessage(String channelId, long messageId) {
        var normalizedChannelId = normalizeChannelId(channelId);
        var messages = channelMessages.getOrDefault(normalizedChannelId, new ConcurrentLinkedDeque<>());

        boolean removed = messages.removeIf(message -> message.getId() == messageId);
        if (!removed) {
            throw new IllegalArgumentException("message not found");
        }
    }

    public List<String> listChannels() {
        return new ArrayList<>(channelOrder);
    }

    public String createChannel(String channelId) {
        var normalized = normalizeChannelId(channelId);
        channelMessages.computeIfAbsent(normalized, key -> new ConcurrentLinkedDeque<>());
        if (!channelOrder.contains(normalized)) {
            channelOrder.add(normalized);
        }
        return normalized;
    }

    public List<String> reorderChannels(List<String> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) {
            throw new IllegalArgumentException("channelIds is required");
        }

        List<String> normalized = channelIds.stream()
                .map(this::normalizeChannelId)
                .distinct()
                .toList();

        if (!normalized.contains(DEFAULT_CHANNEL)) {
            throw new IllegalArgumentException("기본 채널은 제거할 수 없습니다");
        }

        for (String channelId : normalized) {
            createChannel(channelId);
        }

        channelOrder.clear();
        channelOrder.addAll(normalized);
        return listChannels();
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
