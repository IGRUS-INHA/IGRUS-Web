package igrus.web.admin.user.service;

import igrus.web.admin.user.exception.SelfRoleChangeException;
import igrus.web.security.auth.approval.service.manage.ValidateNotLastAdminService;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeUserRoleService {

    private final UserRepository userRepository;
    private final UserRoleHistoryRepository userRoleHistoryRepository;
    private final ValidateNotLastAdminService validateNotLastAdminService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void changeUserRole(Long targetUserId, UserRole newRole, Long currentUserId) {
        log.info("회원 권한 변경 요청: targetUserId={}, newRole={}, currentUserId={}", targetUserId, newRole, currentUserId);

        if (targetUserId.equals(currentUserId)) {
            throw new SelfRoleChangeException();
        }

        validateNotLastAdminService.validateNotLastAdmin(targetUserId);

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        UserRole previousRole = targetUser.getRole();
        targetUser.changeRole(newRole);

        UserRoleHistory history = UserRoleHistory.create(
                targetUser,
                previousRole,
                newRole,
                "관리자에 의한 역할 변경"
        );
        userRoleHistoryRepository.save(history);

        refreshTokenRepository.revokeAllByUserId(targetUserId);
        log.info("권한 변경으로 인한 리프레시 토큰 만료: targetUserId={}", targetUserId);

        eventPublisher.publishEvent(new AccountStatusChangeEvent(
                targetUserId, currentUserId, AccountChangeType.ROLE_CHANGE,
                previousRole.name(), newRole.name(),
                "관리자에 의한 역할 변경"
        ));

        log.info("회원 권한 변경 완료: targetUserId={}, previousRole={}, newRole={}", targetUserId, previousRole, newRole);
    }
}
