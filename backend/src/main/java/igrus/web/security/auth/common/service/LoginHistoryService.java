package igrus.web.security.auth.common.service;

import igrus.web.security.auth.common.domain.LoginFailureReason;
import igrus.web.security.auth.common.domain.LoginHistory;
import igrus.web.security.auth.common.repository.LoginHistoryRepository;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 로그인 히스토리 관리 서비스.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoginHistoryService {

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

    /**
     * 특정 사용자의 로그인 히스토리를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return 로그인 히스토리 페이지
     */
    @Transactional(readOnly = true)
    public Page<LoginHistory> getHistoryByUserId(Long userId, Pageable pageable) {
        return loginHistoryRepository.findByUserIdOrderByAttemptedAtDesc(userId, pageable);
    }

    /**
     * 특정 학번의 로그인 히스토리를 조회합니다.
     *
     * @param studentId 학번
     * @param pageable 페이지 정보
     * @return 로그인 히스토리 페이지
     */
    @Transactional(readOnly = true)
    public Page<LoginHistory> getHistoryByStudentId(String studentId, Pageable pageable) {
        return loginHistoryRepository.findByStudentIdOrderByAttemptedAtDesc(studentId, pageable);
    }

    /**
     * 특정 사용자의 최근 로그인 성공 기록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 최근 로그인 성공 기록 목록 (최대 10건)
     */
    @Transactional(readOnly = true)
    public List<LoginHistory> getRecentSuccessfulLogins(Long userId) {
        return loginHistoryRepository.findTop10ByUserIdAndSuccessTrueOrderByAttemptedAtDesc(userId);
    }

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
