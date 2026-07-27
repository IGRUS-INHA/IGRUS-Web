package igrus.web.security.auth.sso;

/**
 * SSO 코드 교환 결과 (발급된 토큰 묶음)
 */
public record SsoTokenResult(
        String accessToken,
        String refreshToken,
        long accessTokenValidity,
        long refreshTokenValidity
) {
}
