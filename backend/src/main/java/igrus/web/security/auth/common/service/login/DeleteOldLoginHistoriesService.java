package igrus.web.security.auth.common.service.login;

import igrus.web.security.auth.common.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 오래된 로그인 히스토리 삭제 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteOldLoginHistoriesService {

    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * 오래된 로그인 히스토리를 삭제합니다.
     *
     * @param before 이 시각 이전의 히스토리 삭제
     * @return 삭제된 레코드 수
     */
    public int deleteOldHistories(Instant before) {
        int deletedCount = loginHistoryRepository.deleteByAttemptedAtBefore(before);
        log.info("오래된 로그인 히스토리 삭제: {}건, 기준일시={}", deletedCount, before);
        return deletedCount;
    }
}
