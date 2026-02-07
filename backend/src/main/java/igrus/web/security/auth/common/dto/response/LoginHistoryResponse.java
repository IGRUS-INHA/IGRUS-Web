package igrus.web.security.auth.common.dto.response;

import igrus.web.security.auth.common.domain.LoginFailureReason;
import igrus.web.security.auth.common.domain.LoginHistory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "로그인 이력 응답")
public record LoginHistoryResponse(
        @Schema(description = "로그인 이력 ID", example = "1")
        Long id,

        @Schema(description = "사용자 ID (실패 시 null 가능)", example = "1")
        Long userId,

        @Schema(description = "학번", example = "12345678")
        String studentId,

        @Schema(description = "클라이언트 IP 주소", example = "192.168.1.100")
        String ipAddress,

        @Schema(description = "클라이언트 User-Agent", example = "Mozilla/5.0")
        String userAgent,

        @Schema(description = "로그인 성공 여부", example = "true")
        boolean success,

        @Schema(description = "실패 사유 (성공 시 null)", example = "INVALID_CREDENTIALS")
        LoginFailureReason failureReason,

        @Schema(description = "로그인 시도 시각", example = "2024-01-15T10:30:00Z")
        Instant attemptedAt
) {
    public static LoginHistoryResponse from(LoginHistory loginHistory) {
        return new LoginHistoryResponse(
                loginHistory.getId(),
                loginHistory.getUser() != null ? loginHistory.getUser().getId() : null,
                loginHistory.getStudentId(),
                loginHistory.getIpAddress(),
                loginHistory.getUserAgent(),
                loginHistory.isSuccess(),
                loginHistory.getFailureReason(),
                loginHistory.getAttemptedAt()
        );
    }
}
