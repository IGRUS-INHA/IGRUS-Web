package igrus.web.security.auth.password.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 갱신 응답")
public record TokenRefreshResponse(
    @Schema(description = "새로 발급된 JWT Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,

    @Schema(description = "Access Token 만료까지 남은 시간 (초)", example = "3600")
    long expiresIn
) {
    public static TokenRefreshResponse of(String accessToken, long expiresIn) {
        return new TokenRefreshResponse(accessToken, expiresIn);
    }
}
