package igrus.web.security.auth.common.service.login;

import igrus.web.security.auth.common.domain.LoginHistory;
import igrus.web.security.auth.common.repository.LoginHistoryRepository;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 성공 히스토리 기록 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecordLoginSuccessService {

    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * 로그인 성공 히스토리를 기록합니다.
     *
     * @param user 로그인한 사용자
     * @param studentId 학번
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     */
    public void recordSuccess(User user, String studentId, String ipAddress, String userAgent) {
        LoginHistory history = LoginHistory.success(user, studentId, ipAddress, userAgent);
        loginHistoryRepository.save(history);
        log.info("로그인 성공 기록: studentId={}, ip={}", studentId, ipAddress);
    }
}
