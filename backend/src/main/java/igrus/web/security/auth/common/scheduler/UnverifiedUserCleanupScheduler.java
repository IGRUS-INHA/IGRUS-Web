package igrus.web.security.auth.common.scheduler;

import igrus.web.security.auth.common.service.UnverifiedUserCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 미인증 사용자 데이터 정리 스케줄러.
 *
 * <p>매일 새벽 3시에 실행되어 지정된 시간(기본 24시간)이 지난
 * 미인증 이메일 인증 레코드와 관련 사용자 데이터를 정리합니다.</p>
 *
 * <p>주의: {@code @Scheduled}와 {@code @Transactional}을 같은 메서드에 사용하면
 * 프록시 기반 AOP 특성상 트랜잭션이 적용되지 않습니다. 따라서 트랜잭션이 필요한
 * 로직은 별도의 서비스 빈({@link UnverifiedUserCleanupService})으로 분리하여 호출합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnverifiedUserCleanupScheduler {

    private final UnverifiedUserCleanupService cleanupService;

    @Value("${app.cleanup.unverified-user-retention-hours:24}")
    private int retentionHours;

    /**
     * 매일 새벽 3시에 미인증 사용자 데이터를 정리합니다.
     *
     * <p>정리 대상:
     * <ul>
     *   <li>지정 시간이 지난 만료된 미인증 EmailVerification 레코드</li>
     *   <li>이메일 인증을 완료한 적이 없는 사용자와 관련 데이터</li>
     * </ul>
     * </p>
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupUnverifiedUsers() {
        cleanupService.cleanup(retentionHours);
    }
}
