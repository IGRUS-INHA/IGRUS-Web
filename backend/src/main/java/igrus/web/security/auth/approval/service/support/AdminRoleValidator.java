package igrus.web.security.auth.approval.service.support;

import igrus.web.security.auth.approval.exception.AdminRequiredException;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ADMIN 권한 검증 헬퍼.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminRoleValidator {

    private final UserRepository userRepository;

    /**
     * ADMIN 권한을 확인합니다.
     *
     * @param userId 확인할 사용자 ID
     * @throws AdminRequiredException 사용자가 ADMIN이 아닌 경우
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    public void validateAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.isAdmin()) {
            log.warn("ADMIN 권한 검증 실패: userId={}, role={}", userId, user.getRole());
            throw new AdminRequiredException();
        }
    }
}
