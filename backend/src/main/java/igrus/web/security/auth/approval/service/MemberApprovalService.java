package igrus.web.security.auth.approval.service;

import igrus.web.security.auth.approval.dto.response.AssociateInfoResponse;
import igrus.web.security.auth.approval.exception.AdminRequiredException;
import igrus.web.security.auth.approval.exception.BulkApprovalEmptyException;
import igrus.web.security.auth.approval.exception.LastAdminCannotChangeException;
import igrus.web.security.auth.approval.exception.UserNotAssociateException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.repository.UserRoleHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberApprovalService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final UserRoleHistoryRepository userRoleHistoryRepository;

    /**
     * 승인 대기 준회원 목록을 조회합니다.
     *
     * @param pageable 페이지 정보
     * @param approverId 조회 요청자 ID (ADMIN 권한 확인용)
     * @return 준회원 정보 목록 (학번, 본명, 학과, 가입 동기, 가입일 포함)
     * @throws AdminRequiredException 조회 요청자가 ADMIN이 아닌 경우
     */
    @Transactional(readOnly = true)
    public Page<AssociateInfoResponse> getPendingAssociates(Pageable pageable, Long approverId) {
        log.info("준회원 목록 조회 요청: approverId={}", approverId);

        // 1. ADMIN 권한 확인
        validateAdminRole(approverId);

        // 2. ASSOCIATE 역할인 사용자 목록 조회
        Page<User> associates = userRepository.findByRole(UserRole.ASSOCIATE, pageable);

        log.info("준회원 목록 조회 완료: totalElements={}", associates.getTotalElements());

        return associates.map(AssociateInfoResponse::from);
    }

    /**
     * 개별 준회원을 정회원으로 승인합니다.
     *
     * @param userId 승인할 사용자 ID
     * @param approverId 승인 처리자 ID (ADMIN)
     * @throws AdminRequiredException 승인 처리자가 ADMIN이 아닌 경우
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     * @throws UserNotAssociateException 사용자가 ASSOCIATE가 아닌 경우
     */
    public void approveAssociate(Long userId, Long approverId) {
        log.info("개별 승인 요청: userId={}, approverId={}", userId, approverId);

        // 1. ADMIN 권한 확인
        validateAdminRole(approverId);

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. ASSOCIATE 역할 확인
        if (!user.isAssociate()) {
            throw new UserNotAssociateException(userId);
        }

        // 4. 역할 변경 (ASSOCIATE -> MEMBER)
        UserRole previousRole = user.getRole();
        user.promoteToMember();

        // 5. PasswordCredential의 승인 정보 설정
        passwordCredentialRepository.findByUserId(userId)
                .ifPresent(credential -> credential.approve(approverId));

        // 6. 역할 변경 이력 기록
        UserRoleHistory history = UserRoleHistory.create(
                user,
                previousRole,
                UserRole.MEMBER,
                "관리자 승인에 의한 정회원 전환"
        );
        userRoleHistoryRepository.save(history);

        log.info("개별 승인 완료: userId={}, previousRole={}, newRole={}", userId, previousRole, UserRole.MEMBER);
    }

    /**
     * 여러 준회원을 일괄 승인합니다.
     *
     * @param userIds 승인할 사용자 ID 목록
     * @param approverId 승인 처리자 ID (ADMIN)
     * @return 승인된 사용자 수
     * @throws AdminRequiredException 승인 처리자가 ADMIN이 아닌 경우
     * @throws BulkApprovalEmptyException 승인할 사용자 목록이 비어있는 경우
     */
    public int approveBulk(List<Long> userIds, Long approverId) {
        log.info("일괄 승인 요청: userIds={}, approverId={}", userIds, approverId);

        // 1. ADMIN 권한 확인
        validateAdminRole(approverId);

        // 2. 빈 목록 확인
        if (userIds == null || userIds.isEmpty()) {
            throw new BulkApprovalEmptyException();
        }

        // 3. 각 사용자 개별 승인
        int approvedCount = 0;
        List<Long> failedUserIds = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                // 사용자 조회
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    failedUserIds.add(userId);
                    continue;
                }

                // ASSOCIATE인 경우만 승인
                if (!user.isAssociate()) {
                    failedUserIds.add(userId);
                    continue;
                }

                // 역할 변경
                UserRole previousRole = user.getRole();
                user.promoteToMember();

                // PasswordCredential 승인 정보 설정
                passwordCredentialRepository.findByUserId(userId)
                        .ifPresent(credential -> credential.approve(approverId));

                // 역할 변경 이력 기록
                UserRoleHistory history = UserRoleHistory.create(
                        user,
                        previousRole,
                        UserRole.MEMBER,
                        "관리자 일괄 승인에 의한 정회원 전환"
                );
                userRoleHistoryRepository.save(history);

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

    /**
     * ADMIN 권한 확인
     *
     * @param userId 확인할 사용자 ID
     * @throws AdminRequiredException 사용자가 ADMIN이 아닌 경우
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    private void validateAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.isAdmin()) {
            log.warn("ADMIN 권한 검증 실패: userId={}, role={}", userId, user.getRole());
            throw new AdminRequiredException();
        }
    }

    /**
     * 마지막 ADMIN 권한 변경 시도 검증
     * ADMIN이 1명만 남은 경우 권한 변경을 거부합니다.
     *
     * @param userId 권한 변경 대상 사용자 ID
     * @throws LastAdminCannotChangeException 마지막 ADMIN인 경우
     */
    public void validateNotLastAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.isAdmin()) {
            long adminCount = userRepository.countByRole(UserRole.ADMIN);
            if (adminCount <= 1) {
                log.warn("마지막 ADMIN 권한 변경 시도: userId={}", userId);
                throw new LastAdminCannotChangeException();
            }
        }
    }

    /**
     * 여러 ADMIN이 존재하는 경우 특정 ADMIN의 권한을 변경합니다.
     *
     * @param userId 권한 변경 대상 사용자 ID
     * @param newRole 새로운 역할
     * @param changerId 변경 처리자 ID
     * @throws LastAdminCannotChangeException 마지막 ADMIN인 경우
     * @throws AdminRequiredException 변경 처리자가 ADMIN이 아닌 경우
     */
    public void changeAdminRole(Long userId, UserRole newRole, Long changerId) {
        log.info("ADMIN 권한 변경 요청: userId={}, newRole={}, changerId={}", userId, newRole, changerId);

        // 1. 변경 처리자 ADMIN 권한 확인
        validateAdminRole(changerId);

        // 2. 마지막 ADMIN 확인
        validateNotLastAdmin(userId);

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 4. 역할 변경
        UserRole previousRole = user.getRole();
        user.changeRole(newRole);

        // 5. 역할 변경 이력 기록
        UserRoleHistory history = UserRoleHistory.create(
                user,
                previousRole,
                newRole,
                "관리자에 의한 역할 변경"
        );
        userRoleHistoryRepository.save(history);

        log.info("ADMIN 권한 변경 완료: userId={}, previousRole={}, newRole={}", userId, previousRole, newRole);
    }
}
