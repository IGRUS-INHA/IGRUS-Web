package igrus.web.security.auth.password.dto.response;

import igrus.web.user.domain.UserRole;

public record PasswordLoginResponse(
    String accessToken,

    Long userId,

    String studentId,

    String name,

    UserRole role,

    long expiresIn
) {
    public static PasswordLoginResponse of(
            String accessToken,
            Long userId,
            String studentId,
            String name,
            UserRole role,
            long expiresIn
    ) {
        return new PasswordLoginResponse(accessToken, userId, studentId, name, role, expiresIn);
    }
}
