package igrus.web.security.auth.approval.service.write;

import igrus.web.security.auth.approval.exception.BulkApprovalEmptyException;
import igrus.web.security.auth.approval.service.support.AdminRoleValidator;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.repository.UserRoleHistoryRepository;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.event.AccountStatusChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 일괄 준회원 승인 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BulkApproveAssociatesService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final UserRoleHistoryRepository userRoleHistoryRepository;
    private final AdminRoleValidator adminRoleValidator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 여러 준회원을 일괄 승인합니다.
     *
     * @param userIds 승인할 사용자 ID 목록
     * @param approverId 승인 처리자 ID (ADMIN)
     * @return 승인된 사용자 수
     */
    public int approveBulk(List<Long> userIds, Long approverId) {
        log.info("일괄 승인 요청: userIds={}, approverId={}", userIds, approverId);

        adminRoleValidator.validateAdminRole(approverId);

        if (userIds == null || userIds.isEmpty()) {
            throw new BulkApprovalEmptyException();
        }

        int approvedCount = 0;
        List<Long> failedUserIds = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    failedUserIds.add(userId);
                    continue;
                }

                if (!user.isAssociate()) {
                    failedUserIds.add(userId);
                    continue;
                }

                UserRole previousRole = user.getRole();
                user.promoteToMember();

                passwordCredentialRepository.findByUserId(userId)
                        .ifPresent(credential -> credential.approve(approverId));

                UserRoleHistory history = UserRoleHistory.create(
                        user,
                        previousRole,
                        UserRole.MEMBER,
                        "관리자 일괄 승인에 의한 정회원 전환"
                );
                userRoleHistoryRepository.save(history);

                eventPublisher.publishEvent(new AccountStatusChangeEvent(
                        userId, approverId, AccountChangeType.APPROVAL,
                        previousRole.name(), UserRole.MEMBER.name(),
                        "관리자 일괄 승인에 의한 정회원 전환"
                ));

                approvedCount++;
            } catch (Exception e) {
                log.warn("일괄 승인 중 개별 사용자 처리 실패: userId={}, error={}", userId, e.getMessage());
                failedUserIds.add(userId);
            }
        }

        if (!failedUserIds.isEmpty()) {
            log.warn("일괄 승인 중 일부 실패: failedUserIds={}", failedUserIds);
        }

        log.info("일괄 승인 완료: approvedCount={}, failedCount={}", approvedCount, failedUserIds.size());

        return approvedCount;
    }
}
