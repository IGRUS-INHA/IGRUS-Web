package igrus.web.security.auth.common.service.login;

import igrus.web.security.auth.common.domain.LoginAttempt;
import igrus.web.security.auth.common.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 실패 기록 서비스.
 *
 * <p>Brute Force 공격 방지를 위해 로그인 실패 횟수를 추적하고,
 * 일정 횟수 이상 실패 시 계정을 임시로 잠금 처리합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecordFailedAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;

    @Value("${app.security.login-attempts-max:5}")
    private int maxAttempts;

    @Value("${app.security.login-lockout-minutes:10}")
    private int lockoutMinutes;

    /**
     * 로그인 실패를 기록합니다.
     *
     * <p>실패 횟수가 최대 허용 횟수에 도달하면 계정을 잠금 처리합니다.</p>
     * <p>별도 트랜잭션으로 실행되어 호출자 트랜잭션 롤백 시에도 카운트가 유지됩니다.</p>
     *
     * @param studentId 학번
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
}
