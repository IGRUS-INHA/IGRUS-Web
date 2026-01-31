package igrus.web.security.auth.password.dto.internal;

import igrus.web.security.auth.password.dto.response.PasswordLoginResponse;
import igrus.web.user.domain.UserRole;

/**
 * 로그인 결과를 담는 내부 DTO
 * <p>
 * 서비스 레이어에서 컨트롤러로 로그인 결과를 전달할 때 사용합니다.
 * 컨트롤러에서 refreshToken은 쿠키로 설정하고, 나머지는 응답 본문으로 반환합니다.
 *
 * @param accessToken          액세스 토큰
 * @param refreshToken         리프레시 토큰
 * @param userId               사용자 ID
 * @param studentId            학번
 * @param name                 사용자 이름
 * @param role                 사용자 권한
 * @param accessTokenValidity  액세스 토큰 유효 기간 (밀리초)
 * @param refreshTokenValidity 리프레시 토큰 유효 기간 (밀리초)
 */
public record LoginResult(
        String accessToken,
        String refreshToken,
        Long userId,
        String studentId,
        String name,
        UserRole role,
        long accessTokenValidity,
        long refreshTokenValidity
) {
    /**
     * 응답 DTO로 변환합니다 (refreshToken 제외).
     *
     * @return PasswordLoginResponse
     */
    public PasswordLoginResponse toResponse() {
        return PasswordLoginResponse.of(
                accessToken,
                userId,
                studentId,
                name,
                role,
                accessTokenValidity
        );
    }
}
