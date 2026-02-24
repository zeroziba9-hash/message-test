package message.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.regex.Pattern;
import message.common.error.ApiException;
import message.common.error.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9]{4,20}$");
    private static final int PASSWORD_MIN = 4;
    private static final int PASSWORD_MAX = 20;
    private static final int NICKNAME_MIN = 2;
    private static final int NICKNAME_MAX = 20;

    private final JdbcTemplate jdbcTemplate;

    public AuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        seedDefaultAdmin();
    }

    public AuthResponse signup(SignupRequest request) {
        String username = normalize(request.getUsername());
        String password = normalize(request.getPassword());
        String passwordConfirm = normalize(request.getPasswordConfirm());
        String nickname = normalize(request.getNickname());

        validateSignup(username, password, passwordConfirm, nickname);

        if (existsByUsername(username)) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 사용 중인 아이디입니다.");
        }

        jdbcTemplate.update(
                "INSERT INTO users(username, password, nickname, created_at) VALUES(?, ?, ?, ?)",
                username,
                password,
                nickname,
                Timestamp.from(Instant.now()));

        return new AuthResponse(true, "회원가입이 완료되었습니다.", username, nickname);
    }

    public AuthResponse login(LoginRequest request) {
        String username = normalize(request.getUsername());
        String password = normalize(request.getPassword());

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "아이디와 비밀번호를 입력해 주세요.");
        }

        UserAccount account = findByUsername(username);
        if (account == null || !account.password().equals(password)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return new AuthResponse(true, "로그인 성공", account.username(), account.nickname());
    }

    private void seedDefaultAdmin() {
        if (!existsByUsername("admin")) {
            jdbcTemplate.update(
                    "INSERT INTO users(username, password, nickname, created_at) VALUES(?, ?, ?, ?)",
                    "admin",
                    "admin",
                    "관리자",
                    Timestamp.from(Instant.now()));
        }
    }

    private boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                username);
        return count != null && count > 0;
    }

    private UserAccount findByUsername(String username) {
        var rows = jdbcTemplate.query(
                "SELECT username, password, nickname FROM users WHERE username = ?",
                (rs, rowNum) -> new UserAccount(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("nickname")),
                username);

        return rows.isEmpty() ? null : rows.get(0);
    }

    private void validateSignup(String username, String password, String passwordConfirm, String nickname) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "아이디는 4~20자의 소문자, 숫자만 가능합니다.");
        }

        if (password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "비밀번호는 4~20자여야 합니다.");
        }

        if (!password.equals(passwordConfirm)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다.");
        }

        if (nickname == null || nickname.length() < NICKNAME_MIN || nickname.length() > NICKNAME_MAX) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "닉네임은 2자 이상 20자 이하여야 합니다.");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
