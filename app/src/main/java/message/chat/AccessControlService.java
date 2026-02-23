package message.chat;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class AccessControlService {

    private final Map<String, ChatRole> userRoles = new ConcurrentHashMap<>();
    private final Map<String, Map<ChatRole, ChannelPermission>> channelPermissions = new ConcurrentHashMap<>();

    public AccessControlService() {
        userRoles.put("관리자", ChatRole.ADMIN);
    }

    public AccessSummary getSummary(String sender, String channelId) {
        String normalizedSender = normalize(sender, "sender");
        String normalizedChannel = normalize(channelId, "channelId");

        ChatRole role = userRoles.getOrDefault(normalizedSender, ChatRole.MEMBER);
        ChannelPermission permission = resolvePermission(normalizedChannel, role);

        return new AccessSummary(
                normalizedSender,
                normalizedChannel,
                role,
                permission.isCanRead(),
                permission.isCanWrite());
    }

    public AccessSummary setUserRole(String sender, String role) {
        String normalizedSender = normalize(sender, "sender");
        ChatRole parsedRole = ChatRole.from(role);
        userRoles.put(normalizedSender, parsedRole);
        return getSummary(normalizedSender, "general");
    }

    public ChannelPermission setChannelPermission(String channelId, String role, boolean canRead, boolean canWrite) {
        String normalizedChannel = normalize(channelId, "channelId");
        ChatRole parsedRole = ChatRole.from(role);

        if (!canRead && canWrite) {
            throw new IllegalArgumentException("읽기 없이 쓰기만 허용할 수 없습니다");
        }

        channelPermissions.computeIfAbsent(normalizedChannel, key -> new ConcurrentHashMap<>())
                .put(parsedRole, new ChannelPermission(canRead, canWrite));

        return resolvePermission(normalizedChannel, parsedRole);
    }

    private ChannelPermission resolvePermission(String channelId, ChatRole role) {
        Map<ChatRole, ChannelPermission> rolePermissions = channelPermissions
                .computeIfAbsent(channelId, key -> defaultPermissions());

        return rolePermissions.getOrDefault(role, new ChannelPermission(false, false));
    }

    private Map<ChatRole, ChannelPermission> defaultPermissions() {
        Map<ChatRole, ChannelPermission> defaults = new EnumMap<>(ChatRole.class);
        defaults.put(ChatRole.ADMIN, new ChannelPermission(true, true));
        defaults.put(ChatRole.MODERATOR, new ChannelPermission(true, true));
        defaults.put(ChatRole.MEMBER, new ChannelPermission(true, true));
        defaults.put(ChatRole.GUEST, new ChannelPermission(true, false));
        return defaults;
    }

    private String normalize(String value, String fieldName) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }
}
