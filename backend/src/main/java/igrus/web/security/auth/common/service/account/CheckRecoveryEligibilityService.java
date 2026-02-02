package igrus.web.security.auth.common.service.account;

import igrus.web.security.auth.common.dto.response.RecoveryEligibilityResponse;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 복구 가능 여부 확인 서비스.
 * <p>
 * 탈퇴한 계정의 복구 가능 여부를 확인합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CheckRecoveryEligibilityService {

    private final UserRepository userRepository;

    /**
     * 복구 가능 여부를 확인합니다.
     *
     * @param studentId 학번
     * @return 복구 가능 여부 응답
     */
    @Transactional(readOnly = true)
    public RecoveryEligibilityResponse checkRecoveryEligibility(String studentId) {
        log.info("복구 가능 여부 확인: studentId={}", studentId);

        // soft delete 포함하여 사용자 조회
        User user = userRepository.findByStudentIdIncludingDeleted(studentId)
                .orElse(null);

        if (user == null) {
            log.info("사용자 없음: studentId={}", studentId);
            return RecoveryEligibilityResponse.notRecoverable();
        }

        // 탈퇴 상태가 아닌 경우
        if (user.getStatus() != UserStatus.WITHDRAWN) {
            log.info("탈퇴 상태 아님: studentId={}, status={}", studentId, user.getStatus());
            return RecoveryEligibilityResponse.notWithdrawn();
        }

        // soft delete 되지 않은 경우 (논리적 오류)
        if (!user.isDeleted()) {
            log.warn("탈퇴 상태이나 soft delete가 아님: studentId={}", studentId);
            return RecoveryEligibilityResponse.notWithdrawn();
        }

        Instant recoveryDeadline = RecoveryPeriodConstants.getRecoveryDeadline(user);
        Instant now = Instant.now();

        if (now.isAfter(recoveryDeadline)) {
            log.info("복구 기간 만료: studentId={}, deadline={}", studentId, recoveryDeadline);
            return RecoveryEligibilityResponse.notRecoverable();
        }

        log.info("복구 가능: studentId={}, deadline={}", studentId, recoveryDeadline);
        return RecoveryEligibilityResponse.recoverable(recoveryDeadline);
    }
}
