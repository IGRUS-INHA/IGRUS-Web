package igrus.web.security.auth.common.service.login;

import igrus.web.security.auth.common.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 시도 기록 초기화 서비스.
 *
 * <p>로그인 성공 시 호출하여 실패 횟수와 잠금 상태를 초기화합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResetLoginAttemptsService {

    private final LoginAttemptRepository loginAttemptRepository;

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
