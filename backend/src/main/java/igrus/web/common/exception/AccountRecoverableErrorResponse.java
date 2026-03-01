package igrus.web.common.exception;

import java.time.Instant;

/**
 * 복구 가능한 탈퇴 계정에 대한 에러 응답.
 * 클라이언트가 복구 플로우로 이동할 수 있도록 추가 정보를 포함합니다.
 */
public record AccountRecoverableErrorResponse(
        int status,
        String code,
        String message,
        String studentId,
        Instant recoveryDeadline,
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
