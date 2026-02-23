package message.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class ChannelPresenceService {

    private final Map<String, Set<String>> channelUsers = new ConcurrentHashMap<>();
    private final Map<String, SessionPresence> sessionIndex = new ConcurrentHashMap<>();

    public PresenceUpdate join(String channelId, String user, String sessionId) {
        var normalizedChannelId = normalize(channelId, "channelId");
        var normalizedUser = normalize(user, "sender");
        var normalizedSessionId = normalize(sessionId, "sessionId");

        leaveBySession(normalizedSessionId);

        channelUsers.computeIfAbsent(normalizedChannelId, key -> ConcurrentHashMap.newKeySet())
                .add(normalizedUser);
        sessionIndex.put(normalizedSessionId, new SessionPresence(normalizedChannelId, normalizedUser));

        return snapshot(normalizedChannelId);
    }

    public SessionPresence leaveBySession(String sessionId) {
        var normalizedSessionId = normalize(sessionId, "sessionId");
        SessionPresence previous = sessionIndex.remove(normalizedSessionId);
        if (previous == null) {
            return null;
        }

        channelUsers.computeIfPresent(previous.channelId(), (key, users) -> {
            users.remove(previous.user());
            return users.isEmpty() ? null : users;
        });
        return previous;
    }

    public PresenceUpdate snapshot(String channelId) {
        var normalizedChannelId = normalize(channelId, "channelId");
        var users = new ArrayList<>(channelUsers.getOrDefault(normalizedChannelId, Set.of()));
        users.sort(String::compareToIgnoreCase);
        return new PresenceUpdate(normalizedChannelId, users.size(), users);
    }

    public List<String> listUsers(String channelId) {
        return snapshot(channelId).getUsers();
    }

    private String normalize(String value, String fieldName) {
        var normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    public record SessionPresence(String channelId, String user) {
    }
}
