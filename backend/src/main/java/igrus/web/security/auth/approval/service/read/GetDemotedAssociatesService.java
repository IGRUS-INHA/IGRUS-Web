package igrus.web.security.auth.approval.service.read;

import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.domain.AssociateDecisionType;
import igrus.web.security.auth.approval.dto.response.DemotedAssociateInfoResponse;
import igrus.web.security.auth.approval.repository.AssociateDecisionRepository;
import igrus.web.security.auth.approval.service.support.AdminRoleValidator;
import igrus.web.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 강등된 준회원 목록 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetDemotedAssociatesService {

    private final AssociateDecisionRepository associateDecisionRepository;
    private final AdminRoleValidator adminRoleValidator;

    @Transactional(readOnly = true)
    public Page<DemotedAssociateInfoResponse> getDemotedAssociates(Pageable pageable, Long requesterId) {
        log.info("강등된 준회원 목록 조회 요청: requesterId={}", requesterId);

        adminRoleValidator.validateAdminRole(requesterId);

        Page<AssociateDecision> demotedDecisions = associateDecisionRepository.findActiveByType(
                UserRole.ASSOCIATE, AssociateDecisionType.DEMOTED, pageable
        );

        log.info("강등된 준회원 목록 조회 완료: totalElements={}", demotedDecisions.getTotalElements());

        return demotedDecisions.map(DemotedAssociateInfoResponse::from);
    }
}
