package igrus.web.security.auth.password.dto.response;

import igrus.web.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 성공 응답")
public record PasswordLoginResponse(
    @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,

    @Schema(description = "사용자 고유 ID", example = "1")
    Long userId,

    @Schema(description = "학번", example = "12345678")
    String studentId,

    @Schema(description = "사용자 이름", example = "홍길동")
    String name,

    @Schema(description = "사용자 권한", example = "MEMBER", allowableValues = {"ASSOCIATE", "MEMBER", "OPERATOR", "ADMIN"})
    UserRole role,

    @Schema(description = "Access Token 만료까지 남은 시간 (밀리초)", example = "3600000")
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
