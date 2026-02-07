package igrus.web.security.auth.password.service.auth;

import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.exception.token.RefreshTokenExpiredException;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.common.exception.token.RefreshTokenTheftException;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.password.dto.internal.TokenRotationResult;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${app.jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    @Value("${app.jwt.refresh-token-grace-period:10000}")
    private long gracePeriodMillis;

    /**
     * 리프레시 토큰으로 새로운 액세스 토큰을 발급하고, 리프레시 토큰을 로테이션합니다.
     *
     * <p>토큰 로테이션: 매 갱신 시 기존 리프레시 토큰을 폐기하고 새 리프레시 토큰을 발급합니다.
     * <p>탈취 감지: 이미 폐기된 토큰이 사용되면 해당 토큰 패밀리 전체를 무효화합니다.
     * <p>Grace Period: 동시 요청(다중 탭)에 의한 경쟁 조건을 처리합니다.
     *
     * @param refreshTokenValue 리프레시 토큰
     * @return 새로운 액세스 토큰과 리프레시 토큰을 포함한 로테이션 결과
     * @throws RefreshTokenInvalidException 리프레시 토큰이 유효하지 않은 경우
     * @throws RefreshTokenExpiredException 리프레시 토큰이 만료된 경우
     * @throws RefreshTokenTheftException   토큰 도용이 감지된 경우
     */
    @Transactional(noRollbackFor = RefreshTokenTheftException.class)
    public TokenRotationResult refreshToken(String refreshTokenValue) {
        log.info("토큰 갱신 시도");

        // 1. DB에서 Refresh Token 조회 (revoked 포함 - 탈취 감지를 위해)
        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> {
                    log.warn("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰");
                    return new RefreshTokenInvalidException();
                });

        // 2. 이미 폐기된 토큰인 경우 - 탈취 가능성 또는 동시 요청
        if (tokenEntity.isRevoked()) {
            return handleRevokedToken(tokenEntity);
        }

        // 3. 만료 여부 확인
        if (tokenEntity.isExpired()) {
            log.warn("토큰 갱신 실패 - 리프레시 토큰 만료: userId={}", tokenEntity.getUser().getId());
            throw new RefreshTokenExpiredException();
        }

        // 4. 토큰 로테이션 수행
        try {
            return rotateToken(tokenEntity);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("토큰 갱신 실패 - 동시 요청에 의한 충돌: tokenFamily={}", tokenEntity.getTokenFamily(), e);
            throw new RefreshTokenInvalidException();
        }
    }

    /**
     * 이미 폐기된 토큰이 사용된 경우를 처리합니다.
     * Grace Period 내이면 동시 요청으로 간주하고, 아니면 탈취로 감지합니다.
     */
    private TokenRotationResult handleRevokedToken(RefreshToken revokedToken) {
        // 만료된 토큰은 폐기 여부와 관계없이 거부
        if (revokedToken.isExpired()) {
            log.warn("토큰 갱신 실패 - 폐기되고 만료된 리프레시 토큰: userId={}", revokedToken.getUser().getId());
            throw new RefreshTokenExpiredException();
        }

        Duration gracePeriod = Duration.ofMillis(gracePeriodMillis);

        // Grace Period 내: 동시 탭에서의 경쟁 조건으로 간주
        if (revokedToken.isWithinGracePeriod(gracePeriod)) {
            log.debug("유예 기간 내 이미 교체된 토큰 사용 - tokenFamily={}", revokedToken.getTokenFamily());

            RefreshToken activeToken = refreshTokenRepository
                    .findByTokenFamilyAndRevokedFalse(revokedToken.getTokenFamily())
                    .orElseThrow(() -> {
                        log.warn("유예 기간이지만 활성 토큰 없음 - 유효하지 않은 토큰으로 처리");
                        return new RefreshTokenInvalidException();
                    });

            User user = activeToken.getUser();
            String newAccessToken = jwtTokenProvider.createAccessToken(
                    user.getId(), user.getStudentId(), user.getRole().name());

            // Grace Period에서는 Access Token만 갱신, Refresh Token은 반환하지 않음
            return TokenRotationResult.gracePeriod(newAccessToken, accessTokenValidity);
        }

        // Grace Period 밖: 토큰 탈취 감지 → 패밀리 전체 무효화
        int revokedCount = refreshTokenRepository.revokeAllByTokenFamily(revokedToken.getTokenFamily(), Instant.now());
        log.warn("토큰 도용 감지! tokenFamily={}, userId={}, revokedCount={}",
                revokedToken.getTokenFamily(), revokedToken.getUser().getId(), revokedCount);
        throw new RefreshTokenTheftException();
    }

    /**
     * 유효한 토큰의 로테이션을 수행합니다.
     * 기존 토큰을 폐기하고 새 리프레시 토큰과 액세스 토큰을 발급합니다.
     */
    private TokenRotationResult rotateToken(RefreshToken oldToken) {
        User user = oldToken.getUser();

        // 새 리프레시 토큰 JWT 생성
        String newRefreshTokenValue = jwtTokenProvider.createRefreshToken(user.getId());

        // 기존 토큰 폐기 및 교체 토큰 기록
        oldToken.rotateWith(newRefreshTokenValue);

        // 같은 토큰 패밀리로 새 RefreshToken 엔티티 생성
        RefreshToken newTokenEntity = RefreshToken.create(
                user, newRefreshTokenValue, refreshTokenValidity, oldToken.getTokenFamily());
        refreshTokenRepository.save(newTokenEntity);

        // 새 액세스 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getStudentId(), user.getRole().name());

        log.info("토큰 로테이션 성공: userId={}, tokenFamily={}", user.getId(), oldToken.getTokenFamily());

        return new TokenRotationResult(
                newAccessToken, newRefreshTokenValue, accessTokenValidity, refreshTokenValidity);
    }
}
