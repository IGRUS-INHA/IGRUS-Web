package igrus.web.security.auth.approval.service.read;

import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.domain.AssociateDecisionType;
import igrus.web.security.auth.approval.dto.response.RejectedAssociateInfoResponse;
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
 * 거절된 준회원 목록 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetRejectedAssociatesService {

    private final AssociateDecisionRepository associateDecisionRepository;
    private final AdminRoleValidator adminRoleValidator;

    /**
     * 거절된 준회원 목록을 조회합니다.
     *
     * @param pageable 페이지 정보
     * @param requesterId 조회 요청자 ID (ADMIN 권한 확인용)
     * @return 거절된 준회원 정보 목록
     */
    @Transactional(readOnly = true)
    public Page<RejectedAssociateInfoResponse> getRejectedAssociates(Pageable pageable, Long requesterId) {
        log.info("거절된 준회원 목록 조회 요청: requesterId={}", requesterId);

        adminRoleValidator.validateAdminRole(requesterId);

        Page<AssociateDecision> rejectedDecisions = associateDecisionRepository.findByUserRoleAndType(
                UserRole.ASSOCIATE, AssociateDecisionType.REJECTED, pageable
        );

        log.info("거절된 준회원 목록 조회 완료: totalElements={}", rejectedDecisions.getTotalElements());

        return rejectedDecisions.map(RejectedAssociateInfoResponse::from);
    }
}
