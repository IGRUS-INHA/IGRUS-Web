package igrus.web.security.auth.approval.service.read;

import igrus.web.security.auth.approval.domain.AssociateDecisionType;
import igrus.web.security.auth.approval.dto.response.AssociateInfoResponse;
import igrus.web.security.auth.approval.repository.AssociateDecisionRepository;
import igrus.web.security.auth.approval.service.support.AdminRoleValidator;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 승인 대기 준회원 목록 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetPendingAssociatesService {

    private final AssociateDecisionRepository associateDecisionRepository;
    private final AdminRoleValidator adminRoleValidator;

    /**
     * 승인 대기 준회원 목록을 조회합니다.
     *
     * @param pageable 페이지 정보
     * @param approverId 조회 요청자 ID (ADMIN 권한 확인용)
     * @return 준회원 정보 목록
     */
    @Transactional(readOnly = true)
    public Page<AssociateInfoResponse> getPendingAssociates(Pageable pageable, Long approverId) {
        log.info("준회원 목록 조회 요청: approverId={}", approverId);

        adminRoleValidator.validateAdminRole(approverId);

        Page<User> associates = associateDecisionRepository.findPendingAssociates(
                UserRole.ASSOCIATE,
                List.of(AssociateDecisionType.APPROVED, AssociateDecisionType.REJECTED),
                pageable
        );

        log.info("준회원 목록 조회 완료: totalElements={}", associates.getTotalElements());

        return associates.map(AssociateInfoResponse::from);
    }
}
