package message.auth;

public class AuthResponse {
    private boolean success;
    private String message;
    private String username;
    private String nickname;

    public AuthResponse(boolean success, String message, String username, String nickname) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.nickname = nickname;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }
}
