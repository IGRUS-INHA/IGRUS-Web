package igrus.web.security.auth.common.service;

import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 만료된 Refresh Token 정리 서비스.
 *
 * <p>만료된 Refresh Token을 삭제하여 데이터베이스 공간을 확보하고
 * 성능을 유지하는 비즈니스 로직을 담당합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 만료된 Refresh Token을 삭제합니다.
     *
     * @return 삭제된 토큰 수
     */
    public int deleteExpiredTokens() {
        Instant now = Instant.now();
        return refreshTokenRepository.deleteByExpiresAtBefore(now);
    }
}
