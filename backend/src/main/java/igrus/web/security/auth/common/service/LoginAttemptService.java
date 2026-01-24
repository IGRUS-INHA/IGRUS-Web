package igrus.web.security.auth.common.service;

import igrus.web.security.auth.common.domain.LoginAttempt;
import igrus.web.security.auth.common.exception.account.AccountLockedException;
import igrus.web.security.auth.common.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 시도 관리 서비스.
 *
 * <p>Brute Force 공격 방지를 위해 로그인 실패 횟수를 추적하고,
 * 일정 횟수 이상 실패 시 계정을 임시로 잠금 처리합니다.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;

    @Value("${app.security.login-attempts-max:5}")
    private int maxAttempts;

    @Value("${app.security.login-lockout-minutes:30}")
    private int lockoutMinutes;

    /**
     * 계정 잠금 상태를 확인합니다.
     *
     * @param studentId 학번
     * @throws AccountLockedException 계정이 잠금 상태인 경우
     */
    @Transactional(readOnly = true)
    public void checkAccountLocked(String studentId) {
        loginAttemptRepository.findByStudentId(studentId)
                .ifPresent(attempt -> {
                    if (attempt.isLocked()) {
                        log.warn("로그인 시도 - 계정 잠금 상태: studentId={}, lockedUntil={}",
                                studentId, attempt.getLockedUntil());
                        throw new AccountLockedException();
                    }
                });
    }

    /**
     * 로그인 실패를 기록합니다.
     *
     * <p>실패 횟수가 최대 허용 횟수에 도달하면 계정을 잠금 처리합니다.</p>
     *
     * @param studentId 학번
     */
    public void recordFailedAttempt(String studentId) {
        LoginAttempt attempt = loginAttemptRepository.findByStudentId(studentId)
                .orElseGet(() -> LoginAttempt.create(studentId));

        attempt.incrementAttempt();

        if (attempt.getAttemptCount() >= maxAttempts) {
            attempt.lock(lockoutMinutes);
            log.warn("로그인 실패 횟수 초과로 계정 잠금: studentId={}, attemptCount={}, lockoutMinutes={}",
                    studentId, attempt.getAttemptCount(), lockoutMinutes);
        } else {
            log.info("로그인 실패 기록: studentId={}, attemptCount={}/{}",
                    studentId, attempt.getAttemptCount(), maxAttempts);
        }

        loginAttemptRepository.save(attempt);
    }

    /**
     * 로그인 시도 기록을 초기화합니다.
     *
     * <p>로그인 성공 시 호출하여 실패 횟수와 잠금 상태를 초기화합니다.</p>
     *
     * @param studentId 학번
     */
    public void resetAttempts(String studentId) {
        loginAttemptRepository.findByStudentId(studentId)
                .ifPresent(attempt -> {
                    attempt.reset();
                    loginAttemptRepository.save(attempt);
                    log.info("로그인 성공으로 시도 기록 초기화: studentId={}", studentId);
                });
    }
}
