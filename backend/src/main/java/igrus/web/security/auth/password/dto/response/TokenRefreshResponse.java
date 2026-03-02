package igrus.web.security.auth.password.dto.response;

public record TokenRefreshResponse(
    String accessToken,

    long expiresIn
) {
    public static TokenRefreshResponse of(String accessToken, long expiresIn) {
        return new TokenRefreshResponse(accessToken, expiresIn);
    }
}
