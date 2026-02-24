package message.chat;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRepository {

    private final Map<String, Deque<ChatMessage>> channelMessages = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String> channelOrder = new CopyOnWriteArrayList<>();

    public Deque<ChatMessage> getMessages(String channelId) {
        return channelMessages.getOrDefault(channelId, new ConcurrentLinkedDeque<>());
    }

    public void upsertMessages(String channelId, Deque<ChatMessage> messages) {
        channelMessages.put(channelId, messages);
    }

    public void createChannelIfAbsent(String channelId) {
        channelMessages.computeIfAbsent(channelId, key -> new ConcurrentLinkedDeque<>());
        if (!channelOrder.contains(channelId)) {
            channelOrder.add(channelId);
        }
    }

    public List<String> listChannels() {
        return new ArrayList<>(channelOrder);
    }

    public void replaceChannelOrder(List<String> nextOrder) {
        channelOrder.clear();
        channelOrder.addAll(nextOrder);
    }
}
