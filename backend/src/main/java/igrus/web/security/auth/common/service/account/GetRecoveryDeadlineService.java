package igrus.web.security.auth.common.service.account;

import igrus.web.security.auth.common.exception.account.AccountNotRecoverableException;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 복구 가능 기한 조회 서비스.
 * <p>
 * 탈퇴한 계정의 복구 가능 기한을 조회합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetRecoveryDeadlineService {

    private final UserRepository userRepository;

    /**
     * 복구 가능 기한을 조회합니다.
     *
     * @param studentId 학번
     * @return 복구 가능 기한 (탈퇴일 + 5일)
     * @throws InvalidCredentialsException    사용자가 존재하지 않는 경우
     * @throws AccountNotRecoverableException 복구 가능 상태가 아닌 경우
     */
    @Transactional(readOnly = true)
    public Instant getRecoveryDeadline(String studentId) {
        log.info("복구 기한 조회: studentId={}", studentId);

        User user = userRepository.findByStudentIdIncludingDeleted(studentId)
                .orElseThrow(() -> {
                    log.warn("복구 기한 조회 실패 - 사용자 없음: studentId={}", studentId);
                    return new InvalidCredentialsException();
                });

        if (user.getStatus() != UserStatus.WITHDRAWN || !user.isDeleted()) {
            log.warn("복구 기한 조회 실패 - 탈퇴 상태 아님: studentId={}", studentId);
            throw new AccountNotRecoverableException();
        }

        return RecoveryPeriodConstants.getRecoveryDeadline(user);
    }
}
