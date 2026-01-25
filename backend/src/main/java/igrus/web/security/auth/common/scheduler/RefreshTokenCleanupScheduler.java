package igrus.web.security.auth.common.scheduler;

import igrus.web.security.auth.common.service.RefreshTokenCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료된 Refresh Token 정리 스케줄러.
 *
 * <p>매일 새벽 4시에 실행되어 만료된 Refresh Token을 삭제합니다.
 * UnverifiedUserCleanupScheduler는 새벽 3시에 실행되므로, 부하 분산을 위해
 * 1시간 간격을 두고 실행됩니다.</p>
 *
 * <p>주의: {@code @Scheduled}와 {@code @Transactional}을 같은 메서드에 사용하면
 * 프록시 기반 AOP 특성상 트랜잭션이 적용되지 않습니다. 따라서 트랜잭션이 필요한
 * 로직은 별도의 서비스 빈({@link RefreshTokenCleanupService})으로 분리하여 호출합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenCleanupService refreshTokenCleanupService;

    /**
     * 매일 새벽 4시에 만료된 Refresh Token을 정리합니다.
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupExpiredTokens() {
        log.info("만료된 Refresh Token 정리 스케줄러 시작");
        int deletedCount = refreshTokenCleanupService.deleteExpiredTokens();
        log.info("만료된 Refresh Token 정리 완료: {}개 삭제", deletedCount);
    }
}
