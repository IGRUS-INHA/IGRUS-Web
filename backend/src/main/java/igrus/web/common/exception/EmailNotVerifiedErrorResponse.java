package igrus.web.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 이메일 미인증 상태에 대한 에러 응답.
 * 클라이언트가 인증 플로우로 이동할 수 있도록 이메일 정보를 포함합니다.
 */
@Schema(description = "이메일 미인증 에러 응답")
public record EmailNotVerifiedErrorResponse(
        @Schema(description = "HTTP 상태 코드", example = "401")
        int status,

        @Schema(description = "에러 코드", example = "EMAIL_NOT_VERIFIED")
        String code,

        @Schema(description = "에러 메시지", example = "이메일 인증이 완료되지 않았습니다")
        String message,

        @Schema(description = "사용자 등록 이메일", example = "user@inha.edu")
        String email,

        @Schema(description = "에러 발생 시각", example = "2024-01-15T10:30:00Z")
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
