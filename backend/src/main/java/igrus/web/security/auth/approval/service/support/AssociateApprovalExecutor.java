package igrus.web.security.auth.approval.service.support;

import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.repository.AssociateDecisionRepository;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import igrus.web.user.audit.AccountStatusChanged;
import igrus.web.user.repository.UserRoleHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 준회원 승인 핵심 로직 실행기.
 * 개별 승인과 일괄 승인에서 공유되는 단일 사용자 승인 처리를 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssociateApprovalExecutor {

    private final AssociateDecisionRepository associateDecisionRepository;
    private final UserRoleHistoryRepository userRoleHistoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 단일 준회원 승인을 실행합니다.
     * 기존 결정 비활성화, 역할 변경, 결정 기록, 이력 저장, 토큰 만료, 이벤트 발행을 수행합니다.
     *
     * @param user 승인할 사용자 (ASSOCIATE 역할, ACTIVE 상태 검증 완료)
     * @param approverId 승인 처리자 ID
     * @param reason 승인 사유 (역할 변경 이력에 기록)
     */
    public void execute(User user, Long approverId, String reason) {
        associateDecisionRepository.findByUserIdAndActiveTrue(user.getId())
                .ifPresent(AssociateDecision::deactivate);

        UserRole previousRole = user.getRole();
        user.promoteToMember();

        AssociateDecision decision = AssociateDecision.approve(user, approverId);
        associateDecisionRepository.save(decision);

        UserRoleHistory history = UserRoleHistory.create(
                user, previousRole, UserRole.MEMBER, reason
        );
        userRoleHistoryRepository.save(history);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("승인으로 인한 리프레시 토큰 만료: userId={}", user.getId());

        eventPublisher.publishEvent(new AccountStatusChanged(
                user.getId(), approverId, AccountChangeType.APPROVAL,
                previousRole.name(), UserRole.MEMBER.name(), reason
        ));
    }
}
