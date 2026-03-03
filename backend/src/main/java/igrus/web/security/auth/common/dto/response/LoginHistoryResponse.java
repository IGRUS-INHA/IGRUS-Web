package igrus.web.security.auth.common.dto.response;

import igrus.web.security.auth.common.domain.LoginFailureReason;
import igrus.web.security.auth.common.domain.LoginHistory;

import java.time.Instant;

public record LoginHistoryResponse(
        Long id,

        Long userId,

        String studentId,

        String ipAddress,

        String userAgent,

        boolean success,

        LoginFailureReason failureReason,

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
