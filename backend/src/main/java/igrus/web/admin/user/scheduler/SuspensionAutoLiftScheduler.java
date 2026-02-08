package igrus.web.admin.user.scheduler;

import igrus.web.admin.user.service.ChangeUserStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료된 정지를 자동으로 해제하는 스케줄러.
 *
 * <p>매시 정각에 실행되어 관리자가 설정한 정지 종료 시각이 지난 정지를
 * 자동으로 해제합니다.</p>
 *
 * <p>주의: {@code @Scheduled}와 {@code @Transactional}을 같은 메서드에 사용하면
 * 프록시 기반 AOP 특성상 트랜잭션이 적용되지 않습니다. 따라서 트랜잭션이 필요한
 * 로직은 별도의 서비스 빈({@link ChangeUserStatusService})으로 분리하여 호출합니다.</p>
 *
 * @see ChangeUserStatusService#liftExpiredSuspensions()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuspensionAutoLiftScheduler {

    private final ChangeUserStatusService changeUserStatusService;

    /**
     * 매시 정각에 만료된 정지를 자동 해제합니다.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void liftExpiredSuspensions() {
        int processedCount = changeUserStatusService.liftExpiredSuspensions();
        if (processedCount > 0) {
            log.info("자동 정지 해제 완료: {}명 처리", processedCount);
        }
    }
}
