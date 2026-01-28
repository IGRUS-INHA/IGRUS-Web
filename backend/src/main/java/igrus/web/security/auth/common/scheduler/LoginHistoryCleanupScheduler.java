package igrus.web.security.auth.common.scheduler;

import igrus.web.security.auth.common.service.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 로그인 히스토리 정리 스케줄러.
 *
 * <p>1년 이상 된 로그인 히스토리를 매일 새벽 3시에 삭제합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginHistoryCleanupScheduler {

    private static final int RETENTION_DAYS = 365;

    private final LoginHistoryService loginHistoryService;

    /**
     * 1년 이상 된 로그인 히스토리를 삭제합니다.
     * 매일 새벽 3시에 실행됩니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldLoginHistories() {
        log.info("로그인 히스토리 정리 시작");
        Instant cutoffDate = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        int deletedCount = loginHistoryService.deleteOldHistories(cutoffDate);
        log.info("로그인 히스토리 정리 완료: {}건 삭제", deletedCount);
    }
}
