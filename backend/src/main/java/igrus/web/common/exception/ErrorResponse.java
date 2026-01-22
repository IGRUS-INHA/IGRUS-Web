package igrus.web.common.exception;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String code,
        String message,
        Instant timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage(),
                Instant.now()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                errorCode.getStatus(),
                errorCode.getCode(),
                message,
                Instant.now()
        );
    }

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(
                status,
                code,
                message,
                Instant.now()
        );
    }
}
