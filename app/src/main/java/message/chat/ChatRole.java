package message.chat;

public enum ChatRole {
    ADMIN,
    MODERATOR,
    MEMBER,
    GUEST;

    public static ChatRole from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("role is required");
        }
        try {
            return ChatRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid role: " + value);
        }
    }
}
