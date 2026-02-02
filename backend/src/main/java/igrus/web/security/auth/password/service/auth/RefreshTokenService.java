package igrus.web.security.auth.password.service.auth;

import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.exception.token.RefreshTokenExpiredException;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.password.dto.response.TokenRefreshResponse;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.access-token-validity}")
    private long accessTokenValidity;

    /**
     * 리프레시 토큰으로 새로운 액세스 토큰을 발급합니다.
     *
     * @param refreshTokenValue 리프레시 토큰
     * @return 새로운 액세스 토큰 응답
     * @throws RefreshTokenInvalidException 리프레시 토큰이 유효하지 않은 경우
     * @throws RefreshTokenExpiredException 리프레시 토큰이 만료된 경우
     */
    @Transactional(readOnly = true)
    public TokenRefreshResponse refreshToken(String refreshTokenValue) {
        log.info("토큰 갱신 시도");

        // 1. DB에서 Refresh Token 조회 (revoked가 아닌 토큰)
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> {
                    log.warn("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰");
                    return new RefreshTokenInvalidException();
                });

        // 2. 만료 여부 확인
        if (refreshTokenEntity.isExpired()) {
            log.warn("토큰 갱신 실패 - 리프레시 토큰 만료: userId={}", refreshTokenEntity.getUser().getId());
            throw new RefreshTokenExpiredException();
        }

        // 3. 새 Access Token 발급
        User user = refreshTokenEntity.getUser();
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getStudentId(),
                user.getRole().name()
        );

        log.info("토큰 갱신 성공: userId={}", user.getId());

        return TokenRefreshResponse.of(newAccessToken, accessTokenValidity);
    }
}
