package igrus.web.security.auth.approval.service.write;

import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.exception.AssociateAlreadyDecidedException;
import igrus.web.security.auth.approval.exception.UserNotAssociateException;
import igrus.web.security.auth.common.exception.email.EmailNotVerifiedException;
import igrus.web.security.auth.approval.repository.AssociateDecisionRepository;
import igrus.web.security.auth.approval.service.support.AdminRoleValidator;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개별 준회원 거절 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RejectAssociateService {

    private final UserRepository userRepository;
    private final AssociateDecisionRepository associateDecisionRepository;
    private final AdminRoleValidator adminRoleValidator;

    /**
     * 개별 준회원을 거절합니다.
     *
     * @param userId 거절할 사용자 ID
     * @param rejectorId 거절 처리자 ID (ADMIN)
     * @param reason 거절 사유
     */
    public void rejectAssociate(Long userId, Long rejectorId, String reason) {
        log.info("개별 거절 요청: userId={}, rejectorId={}", userId, rejectorId);

        adminRoleValidator.validateAdminRole(rejectorId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.isAssociate()) {
            throw new UserNotAssociateException(userId);
        }

        if (user.isPendingVerification()) {
            throw new EmailNotVerifiedException();
        }

        associateDecisionRepository.findByUserIdAndActiveTrue(userId)
                .ifPresent(decision -> {
                    if (decision.isDemoted()) {
                        decision.deactivate();
                    } else {
                        throw new AssociateAlreadyDecidedException();
                    }
                });

        AssociateDecision decision = AssociateDecision.reject(user, rejectorId, reason);
        associateDecisionRepository.save(decision);

        log.info("개별 거절 완료: userId={}, reason={}", userId, reason);
    }
}
