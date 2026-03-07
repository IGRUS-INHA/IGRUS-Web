package igrus.web.admin.user.service;

import igrus.web.admin.user.exception.ForceWithdrawException;
import igrus.web.admin.user.exception.SelfStatusChangeException;
import igrus.web.security.auth.common.exception.account.AccountWithdrawnException;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.audit.AccountStatusChanged;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.withdrawal.domain.WithdrawalLog;
import igrus.web.user.withdrawal.repository.WithdrawalLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ForceWithdrawService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final WithdrawalLogRepository withdrawalLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void forceWithdraw(Long targetUserId, String reason, Long currentUserId) {
        log.info("강제 탈퇴 요청 - targetUserId: {}, performedBy: {}", targetUserId, currentUserId);

        // 1. 자기 자신 강제 탈퇴 방지
        if (targetUserId.equals(currentUserId)) {
            throw new SelfStatusChangeException();
        }

        // 2. 대상 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        // 3. 이미 탈퇴한 사용자 확인
        if (targetUser.isWithdrawn()) {
            throw new AccountWithdrawnException();
        }

        // 4. 마지막 ADMIN 보호
        if (targetUser.isAdmin()) {
            long adminCount = userRepository.countByRole(UserRole.ADMIN);
            if (adminCount <= 1) {
                throw ForceWithdrawException.lastAdminCannotWithdraw();
            }
        }

        String previousStatus = targetUser.getStatus().name();

        // 5. 탈퇴 로그 저장
        WithdrawalLog withdrawalLog = WithdrawalLog.create(targetUser, reason);
        withdrawalLogRepository.save(withdrawalLog);

        // 6. User 상태 WITHDRAWN + soft delete
        targetUser.withdraw();
        targetUser.delete(currentUserId);

        // 7. PasswordCredential 상태 WITHDRAWN + soft delete
        passwordCredentialRepository.findByUserId(targetUserId)
                .ifPresent(credential -> {
                    credential.withdraw();
                    credential.delete(currentUserId);
                });

        // 8. RefreshToken 전부 무효화
        refreshTokenRepository.revokeAllByUserId(targetUserId);

        // 9. 감사 이력 이벤트 발행
        eventPublisher.publishEvent(new AccountStatusChanged(
                targetUserId, currentUserId, AccountChangeType.FORCE_WITHDRAWAL,
                previousStatus, UserStatus.WITHDRAWN.name(),
                reason
        ));

        log.info("강제 탈퇴 완료 - targetUserId: {}, performedBy: {}", targetUserId, currentUserId);
    }
}
