package igrus.web.security.auth.common.service.login;

import igrus.web.security.auth.common.domain.LoginFailureReason;
import igrus.web.security.auth.common.domain.LoginHistory;
import igrus.web.security.auth.common.repository.LoginHistoryRepository;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 실패 히스토리 기록 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecordLoginFailureService {

    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * 로그인 실패 히스토리를 기록합니다 (사용자 정보 없이).
     *
     * @param studentId 시도한 학번
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @param failureReason 실패 사유
     */
    public void recordFailure(String studentId, String ipAddress, String userAgent,
                              LoginFailureReason failureReason) {
        LoginHistory history = LoginHistory.failure(studentId, ipAddress, userAgent, failureReason);
        loginHistoryRepository.save(history);
        log.info("로그인 실패 기록: studentId={}, ip={}, reason={}", studentId, ipAddress, failureReason);
    }

    /**
     * 로그인 실패 히스토리를 기록합니다 (사용자 정보 포함).
     *
     * @param user 로그인 시도한 사용자
     * @param studentId 시도한 학번
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @param failureReason 실패 사유
     */
    public void recordFailure(User user, String studentId, String ipAddress, String userAgent,
                              LoginFailureReason failureReason) {
        LoginHistory history = LoginHistory.failure(user, studentId, ipAddress, userAgent, failureReason);
        loginHistoryRepository.save(history);
        log.info("로그인 실패 기록: studentId={}, userId={}, ip={}, reason={}",
                studentId, user.getId(), ipAddress, failureReason);
    }
}
