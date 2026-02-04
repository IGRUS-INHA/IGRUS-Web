package igrus.web.security.auth.common.service.account;

import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 재가입 가능 여부 확인 서비스.
 * <p>
 * 주어진 학번으로 최근 탈퇴 이력이 있는지 확인합니다 (재가입 제한 체크용).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CheckReRegistrationEligibilityService {

    private final UserRepository userRepository;

    /**
     * 주어진 학번으로 최근 탈퇴 이력이 있는지 확인합니다 (재가입 제한 체크용).
     *
     * @param studentId 학번
     * @return 재가입 가능 여부 및 재가입 가능 시점
     */
    @Transactional(readOnly = true)
    public ReRegistrationCheckResult checkReRegistrationEligibility(String studentId) {
        log.info("재가입 가능 여부 확인: studentId={}", studentId);

        User user = userRepository.findByStudentIdIncludingDeleted(studentId)
                .orElse(null);

        // 사용자가 존재하지 않으면 재가입 가능
        if (user == null) {
            return ReRegistrationCheckResult.eligible();
        }

        // 탈퇴 상태가 아니면 이미 가입된 상태
        if (user.getStatus() != UserStatus.WITHDRAWN) {
            return ReRegistrationCheckResult.alreadyRegistered();
        }

        // soft delete가 아니면 논리적 오류
        if (!user.isDeleted()) {
            return ReRegistrationCheckResult.alreadyRegistered();
        }

        Instant recoveryDeadline = RecoveryPeriodConstants.getRecoveryDeadline(user);
        Instant now = Instant.now();

        // 복구 기간 내이면 재가입 불가
        if (now.isBefore(recoveryDeadline)) {
            log.info("재가입 불가 - 복구 기간 내: studentId={}, reRegistrationAvailableAt={}", studentId, recoveryDeadline);
            return ReRegistrationCheckResult.restricted(recoveryDeadline);
        }

        // 복구 기간 만료 후 재가입 가능
        return ReRegistrationCheckResult.eligible();
    }
}
