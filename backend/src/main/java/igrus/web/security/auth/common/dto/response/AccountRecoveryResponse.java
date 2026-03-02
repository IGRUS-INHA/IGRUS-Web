package igrus.web.security.auth.common.dto.response;

import igrus.web.user.domain.UserRole;

public record AccountRecoveryResponse(
    String accessToken,

    Long userId,

    String studentId,

    String name,

    UserRole role,

    long expiresIn,

    String message
) {
    public static AccountRecoveryResponse of(
            String accessToken,
            Long userId,
            String studentId,
            String name,
            UserRole role,
            long expiresIn
    ) {
        return new AccountRecoveryResponse(
            accessToken,
            userId,
            studentId,
            name,
            role,
            expiresIn,
            "계정이 성공적으로 복구되었습니다"
        );
    }
}
