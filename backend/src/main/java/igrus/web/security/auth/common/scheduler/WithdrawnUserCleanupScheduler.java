package igrus.web.security.auth.common.scheduler;

import igrus.web.security.auth.common.service.WithdrawnUserCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 탈퇴 사용자 인증 데이터 정리 스케줄러.
 *
 * <p>매일 새벽 5시에 실행되어 탈퇴 후 복구 가능 기간(5일)이 경과한
 * 사용자의 인증 데이터를 영구 삭제합니다.</p>
 *
 * <p>주의: {@code @Scheduled}와 {@code @Transactional}을 같은 메서드에 사용하면
 * 프록시 기반 AOP 특성상 트랜잭션이 적용되지 않습니다. 따라서 트랜잭션이 필요한
 * 로직은 별도의 서비스 빈({@link WithdrawnUserCleanupService})으로 분리하여 호출합니다.</p>
 *
 * @see WithdrawnUserCleanupService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnUserCleanupScheduler {

    private final WithdrawnUserCleanupService withdrawnUserCleanupService;

    /**
     * 매일 새벽 5시에 탈퇴 사용자 인증 데이터를 정리합니다.
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void cleanupWithdrawnUsers() {
        log.info("탈퇴 사용자 인증 데이터 정리 스케줄러 시작");
        int processedCount = withdrawnUserCleanupService.cleanupExpiredWithdrawnUsers();
        log.info("탈퇴 사용자 인증 데이터 정리 완료: {}명 처리", processedCount);
    }
}
