package igrus.web.security.auth.approval.service.write;

import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.exception.AssociateAlreadyDecidedException;
import igrus.web.security.auth.approval.exception.UserNotAssociateException;
import igrus.web.security.auth.approval.repository.AssociateDecisionRepository;
import igrus.web.security.auth.approval.service.support.AdminRoleValidator;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.repository.UserRoleHistoryRepository;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.event.AccountStatusChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개별 준회원 승인 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApproveAssociateService {

    private final UserRepository userRepository;
    private final AssociateDecisionRepository associateDecisionRepository;
    private final UserRoleHistoryRepository userRoleHistoryRepository;
    private final AdminRoleValidator adminRoleValidator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 개별 준회원을 정회원으로 승인합니다.
     *
     * @param userId 승인할 사용자 ID
     * @param approverId 승인 처리자 ID (ADMIN)
     */
    public void approveAssociate(Long userId, Long approverId) {
        log.info("개별 승인 요청: userId={}, approverId={}", userId, approverId);

        adminRoleValidator.validateAdminRole(approverId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.isAssociate()) {
            throw new UserNotAssociateException(userId);
        }

        if (associateDecisionRepository.findByUserId(userId).isPresent()) {
            throw new AssociateAlreadyDecidedException();
        }

        UserRole previousRole = user.getRole();
        user.promoteToMember();

        AssociateDecision decision = AssociateDecision.approve(user, approverId);
        associateDecisionRepository.save(decision);

        UserRoleHistory history = UserRoleHistory.create(
                user,
                previousRole,
                UserRole.MEMBER,
                "관리자 승인에 의한 정회원 전환"
        );
        userRoleHistoryRepository.save(history);

        eventPublisher.publishEvent(new AccountStatusChangeEvent(
                userId, approverId, AccountChangeType.APPROVAL,
                previousRole.name(), UserRole.MEMBER.name(),
                "관리자 승인에 의한 정회원 전환"
        ));

        log.info("개별 승인 완료: userId={}, previousRole={}, newRole={}", userId, previousRole, UserRole.MEMBER);
    }
}
