package igrus.web.security.auth.common.service.login;

import igrus.web.security.auth.common.exception.account.AccountLockedException;
import igrus.web.security.auth.common.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정 잠금 상태 확인 서비스.
 *
 * <p>Brute Force 공격 방지를 위해 계정의 잠금 상태를 확인합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CheckAccountLockedService {

    private final LoginAttemptRepository loginAttemptRepository;

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
}
