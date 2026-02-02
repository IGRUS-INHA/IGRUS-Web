package igrus.web.security.auth.password.service.auth;

import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LogoutService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 로그아웃을 수행합니다.
     *
     * @param refreshTokenValue 리프레시 토큰
     * @throws RefreshTokenInvalidException 리프레시 토큰이 유효하지 않은 경우
     */
    public void logout(String refreshTokenValue) {
        log.info("로그아웃 시도");

        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> {
                    log.warn("로그아웃 실패 - 유효하지 않은 리프레시 토큰");
                    return new RefreshTokenInvalidException();
                });

        refreshToken.revoke();

        log.info("로그아웃 성공: userId={}", refreshToken.getUser().getId());
    }
}
