package igrus.web.storage.scheduler;

import igrus.web.storage.service.FileExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * REQUESTED 상태에서 24시간 경과된 파일 메타데이터를 EXPIRED로 전환하는 스케줄러.
 *
 * <p>매시 정각에 실행되어 고아 파일의 메타데이터를 정리한다.</p>
 *
 * <p>주의: {@code @Scheduled}와 {@code @Transactional}을 같은 메서드에 사용하면
 * 프록시 기반 AOP 특성상 트랜잭션이 적용되지 않습니다. 따라서 트랜잭션이 필요한
 * 로직은 별도의 서비스 빈({@link FileExpirationService})으로 분리하여 호출합니다.</p>
 *
 * @see FileExpirationService#expireStaleRequests()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileExpirationScheduler {

    private final FileExpirationService fileExpirationService;

    /**
     * 매시 정각에 만료 대상 파일 메타데이터를 EXPIRED로 전환합니다.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void expireStaleRequests() {
        int processedCount = fileExpirationService.expireStaleRequests();
        if (processedCount > 0) {
            log.info("파일 만료 처리 완료: {}건 EXPIRED 전환", processedCount);
        }
    }
}
