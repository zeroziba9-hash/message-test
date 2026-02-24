package message.chat;

import message.auth.AuthService;
import message.common.error.ApiException;
import message.common.error.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccessControlService {

    private final JdbcTemplate jdbcTemplate;

    public AccessControlService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        seedDefaults();
    }

    public AccessSummary getSummary(String sender, String channelId) {
        String normalizedSender = normalize(sender, "sender");
        String normalizedChannel = normalize(channelId, "channelId");

        ChatRole role = findRole(normalizedSender);
        ChannelPermission permission = findChannelPermission(normalizedChannel, role);

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

        if (existsRole(normalizedSender)) {
            jdbcTemplate.update("UPDATE user_roles SET role = ? WHERE sender = ?", parsedRole.name(), normalizedSender);
        } else {
            jdbcTemplate.update("INSERT INTO user_roles(sender, role) VALUES(?, ?)", normalizedSender, parsedRole.name());
        }

        return getSummary(normalizedSender, ChatService.DEFAULT_CHANNEL);
    }

    public ChannelPermission setChannelPermission(String channelId, String role, boolean canRead, boolean canWrite) {
        String normalizedChannel = normalize(channelId, "channelId");
        ChatRole parsedRole = ChatRole.from(role);

        if (!canRead && canWrite) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "읽기 없이 쓰기만 허용할 수 없습니다");
        }

        if (existsPermission(normalizedChannel, parsedRole)) {
            jdbcTemplate.update(
                    "UPDATE channel_permissions SET can_read = ?, can_write = ? WHERE channel_id = ? AND role = ?",
                    canRead,
                    canWrite,
                    normalizedChannel,
                    parsedRole.name());
        } else {
            jdbcTemplate.update(
                    "INSERT INTO channel_permissions(channel_id, role, can_read, can_write) VALUES(?, ?, ?, ?)",
                    normalizedChannel,
                    parsedRole.name(),
                    canRead,
                    canWrite);
        }

        return findChannelPermission(normalizedChannel, parsedRole);
    }

    private boolean existsRole(String sender) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_roles WHERE sender = ?", Integer.class, sender);
        return count != null && count > 0;
    }

    private boolean existsPermission(String channelId, ChatRole role) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM channel_permissions WHERE channel_id = ? AND role = ?",
                Integer.class,
                channelId,
                role.name());
        return count != null && count > 0;
    }

    private ChatRole findRole(String sender) {
        // Safety rule: built-in admin account must always keep ADMIN role.
        if (AuthService.ROOT_ADMIN_USERNAME.equalsIgnoreCase(sender)) {
            return ChatRole.ADMIN;
        }

        var rows = jdbcTemplate.query(
                "SELECT role FROM user_roles WHERE sender = ?",
                (rs, rowNum) -> ChatRole.from(rs.getString("role")),
                sender);

        return rows.isEmpty() ? ChatRole.MEMBER : rows.get(0);
    }

    private ChannelPermission findChannelPermission(String channelId, ChatRole role) {
        var rows = jdbcTemplate.query(
                "SELECT can_read, can_write FROM channel_permissions WHERE channel_id = ? AND role = ?",
                (rs, rowNum) -> new ChannelPermission(rs.getBoolean("can_read"), rs.getBoolean("can_write")),
                channelId,
                role.name());

        if (!rows.isEmpty()) {
            return rows.get(0);
        }

        return defaultPermission(role);
    }

    private ChannelPermission defaultPermission(ChatRole role) {
        return switch (role) {
            case ADMIN, MODERATOR, MEMBER -> new ChannelPermission(true, true);
            case GUEST -> new ChannelPermission(true, false);
        };
    }

    private void seedDefaults() {
        // Remove legacy admin role mapping.
        jdbcTemplate.update("DELETE FROM user_roles WHERE sender = ?", "admin");

        if (existsRole(AuthService.ROOT_ADMIN_USERNAME)) {
            jdbcTemplate.update(
                    "UPDATE user_roles SET role = ? WHERE sender = ?",
                    ChatRole.ADMIN.name(),
                    AuthService.ROOT_ADMIN_USERNAME);
        } else {
            jdbcTemplate.update(
                    "INSERT INTO user_roles(sender, role) VALUES(?, ?)",
                    AuthService.ROOT_ADMIN_USERNAME,
                    ChatRole.ADMIN.name());
        }

        Integer channelCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM channels WHERE channel_id = ?",
                Integer.class,
                ChatService.DEFAULT_CHANNEL);
        if (channelCount == null || channelCount == 0) {
            jdbcTemplate.update("INSERT INTO channels(channel_id, sort_order) VALUES(?, 1)", ChatService.DEFAULT_CHANNEL);
        }

        for (ChatRole role : ChatRole.values()) {
            if (!existsPermission(ChatService.DEFAULT_CHANNEL, role)) {
                ChannelPermission p = defaultPermission(role);
                jdbcTemplate.update(
                        "INSERT INTO channel_permissions(channel_id, role, can_read, can_write) VALUES(?, ?, ?, ?)",
                        ChatService.DEFAULT_CHANNEL,
                        role.name(),
                        p.isCanRead(),
                        p.isCanWrite());
            }
        }
    }

    private String normalize(String value, String fieldName) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, fieldName + " is required");
        }
        return normalized;
    }
}
