package igrus.web.security.auth.password.dto.internal;

import igrus.web.security.auth.password.dto.response.TokenRefreshResponse;

import java.util.Objects;

/**
 * 토큰 로테이션 결과를 담는 내부 DTO.
 * <p>
 * 서비스 레이어에서 컨트롤러로 토큰 갱신 결과를 전달할 때 사용합니다.
 * 컨트롤러에서 newRefreshToken은 쿠키로 설정하고, accessToken은 응답 본문으로 반환합니다.
 *
 * @param accessToken          새로 발급된 액세스 토큰
 * @param newRefreshToken      새로 발급된 리프레시 토큰 (Grace Period 시 null)
 * @param accessTokenValidity  액세스 토큰 유효 기간 (밀리초)
 * @param refreshTokenValidity 리프레시 토큰 유효 기간 (밀리초, Grace Period 시 0)
 */
public record TokenRotationResult(
        String accessToken,
        String newRefreshToken,
        long accessTokenValidity,
        long refreshTokenValidity
) {
    public TokenRotationResult {
        Objects.requireNonNull(accessToken, "accessToken must not be null");
        if (accessTokenValidity <= 0) {
            throw new IllegalArgumentException("accessTokenValidity must be positive");
        }
    }

    /**
     * Grace Period용 결과를 생성합니다 (Access Token만 갱신).
     *
     * @param accessToken         새로 발급된 액세스 토큰
     * @param accessTokenValidity 액세스 토큰 유효 기간 (밀리초)
     * @return Grace Period용 TokenRotationResult
     */
    public static TokenRotationResult gracePeriod(String accessToken, long accessTokenValidity) {
        return new TokenRotationResult(accessToken, null, accessTokenValidity, 0);
    }

    /**
     * 응답 DTO로 변환합니다 (refreshToken 제외).
     *
     * @return TokenRefreshResponse
     */
    public TokenRefreshResponse toResponse() {
        return TokenRefreshResponse.of(accessToken, accessTokenValidity);
    }
}
