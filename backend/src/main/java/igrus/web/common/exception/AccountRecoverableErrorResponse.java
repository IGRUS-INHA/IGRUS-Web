package igrus.web.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 복구 가능한 탈퇴 계정에 대한 에러 응답.
 * 클라이언트가 복구 플로우로 이동할 수 있도록 추가 정보를 포함합니다.
 */
@Schema(description = "복구 가능한 탈퇴 계정 에러 응답")
public record AccountRecoverableErrorResponse(
        @Schema(description = "HTTP 상태 코드", example = "200")
        int status,

        @Schema(description = "에러 코드", example = "ACCOUNT_RECOVERABLE")
        String code,

        @Schema(description = "안내 메시지", example = "복구 가능한 탈퇴 계정입니다")
        String message,

        @Schema(description = "학번", example = "12345678")
        String studentId,

        @Schema(description = "복구 가능 마감 시한", example = "2024-02-15T10:30:00Z")
        Instant recoveryDeadline,

        @Schema(description = "에러 발생 시각", example = "2024-01-15T10:30:00Z")
        Instant timestamp
) {

    public static AccountRecoverableErrorResponse of(ErrorCode errorCode, String studentId, Instant recoveryDeadline) {
        return new AccountRecoverableErrorResponse(
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage(),
                studentId,
                recoveryDeadline,
                Instant.now()
        );
    }
}
