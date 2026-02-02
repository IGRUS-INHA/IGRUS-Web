package igrus.web.security.auth.approval.service.manage;

import igrus.web.security.auth.approval.service.support.AdminRoleValidator;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.repository.UserRoleHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADMIN 권한 변경 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeAdminRoleService {

    private final UserRepository userRepository;
    private final UserRoleHistoryRepository userRoleHistoryRepository;
    private final AdminRoleValidator adminRoleValidator;
    private final ValidateNotLastAdminService validateNotLastAdminService;

    /**
     * 여러 ADMIN이 존재하는 경우 특정 ADMIN의 권한을 변경합니다.
     *
     * @param userId 권한 변경 대상 사용자 ID
     * @param newRole 새로운 역할
     * @param changerId 변경 처리자 ID
     */
    public void changeAdminRole(Long userId, UserRole newRole, Long changerId) {
        log.info("ADMIN 권한 변경 요청: userId={}, newRole={}, changerId={}", userId, newRole, changerId);

        adminRoleValidator.validateAdminRole(changerId);

        validateNotLastAdminService.validateNotLastAdmin(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        UserRole previousRole = user.getRole();
        user.changeRole(newRole);

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
