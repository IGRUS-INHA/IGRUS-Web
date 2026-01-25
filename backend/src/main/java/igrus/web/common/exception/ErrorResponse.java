package igrus.web.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(description = "HTTP 상태 코드", example = "400")
        int status,

        @Schema(description = "에러 코드", example = "AUTH_001")
        String code,

        @Schema(description = "에러 메시지", example = "잘못된 요청입니다.")
        String message,

        @Schema(description = "에러 발생 시각", example = "2024-01-15T10:30:00Z")
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
