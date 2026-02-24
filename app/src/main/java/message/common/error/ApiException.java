package message.common.error;

import java.util.List;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<String> details;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public ApiException(ErrorCode errorCode, String message, List<String> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<String> getDetails() {
        return details;
    }
}
