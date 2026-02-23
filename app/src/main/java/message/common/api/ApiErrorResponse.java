package message.common.api;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<String> details) {

    public static ApiErrorResponse of(int status, String code, String message, List<String> details) {
        return new ApiErrorResponse(Instant.now(), status, code, message, details);
    }
}
