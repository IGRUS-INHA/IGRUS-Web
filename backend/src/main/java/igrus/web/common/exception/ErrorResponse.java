package igrus.web.common.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String code,
        String message,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage(),
                LocalDateTime.now()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                errorCode.getStatus(),
                errorCode.getCode(),
                message,
                LocalDateTime.now()
        );
    }

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(
                status,
                code,
                message,
                LocalDateTime.now()
        );
    }
}
