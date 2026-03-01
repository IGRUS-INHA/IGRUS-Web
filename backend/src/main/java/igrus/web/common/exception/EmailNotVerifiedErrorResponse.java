package igrus.web.common.exception;

import java.time.Instant;

/**
 * 이메일 미인증 상태에 대한 에러 응답.
 * 클라이언트가 인증 플로우로 이동할 수 있도록 이메일 정보를 포함합니다.
 */
public record EmailNotVerifiedErrorResponse(
        int status,
        String code,
        String message,
        String email,
        Instant timestamp
) {

    public static EmailNotVerifiedErrorResponse of(ErrorCode errorCode, String email) {
        return new EmailNotVerifiedErrorResponse(
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage(),
                email,
                Instant.now()
        );
    }
}
