package igrus.web.security.auth.approval.service.write;

import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.exception.BulkRejectionEmptyException;
import igrus.web.security.auth.approval.repository.AssociateDecisionRepository;
import igrus.web.security.auth.approval.service.support.AdminRoleValidator;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 일괄 준회원 거절 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BulkRejectAssociatesService {

    private final UserRepository userRepository;
    private final AssociateDecisionRepository associateDecisionRepository;
    private final AdminRoleValidator adminRoleValidator;

    /**
     * 여러 준회원을 일괄 거절합니다.
     *
     * @param userIds 거절할 사용자 ID 목록
     * @param rejectorId 거절 처리자 ID (ADMIN)
     * @param reason 거절 사유
     * @return 거절된 사용자 수
     */
    public int rejectBulk(List<Long> userIds, Long rejectorId, String reason) {
        log.info("일괄 거절 요청: userIds={}, rejectorId={}", userIds, rejectorId);

        adminRoleValidator.validateAdminRole(rejectorId);

        if (userIds == null || userIds.isEmpty()) {
            throw new BulkRejectionEmptyException();
        }

        int rejectedCount = 0;
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

                if (associateDecisionRepository.findByUserId(userId).isPresent()) {
                    failedUserIds.add(userId);
                    continue;
                }

                AssociateDecision decision = AssociateDecision.reject(user, rejectorId, reason);
                associateDecisionRepository.save(decision);

                rejectedCount++;
            } catch (Exception e) {
                log.warn("일괄 거절 중 개별 사용자 처리 실패: userId={}, error={}", userId, e.getMessage());
                failedUserIds.add(userId);
            }
        }

        if (!failedUserIds.isEmpty()) {
            log.warn("일괄 거절 중 일부 실패: failedUserIds={}", failedUserIds);
        }

        log.info("일괄 거절 완료: rejectedCount={}, failedCount={}", rejectedCount, failedUserIds.size());

        return rejectedCount;
    }
}
