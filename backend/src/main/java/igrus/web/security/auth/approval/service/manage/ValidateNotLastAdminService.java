package igrus.web.security.auth.approval.service.manage;

import igrus.web.security.auth.approval.exception.LastAdminCannotChangeException;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마지막 ADMIN 검증 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ValidateNotLastAdminService {

    private final UserRepository userRepository;

    /**
     * 마지막 ADMIN 권한 변경 시도를 검증합니다.
     * ADMIN이 1명만 남은 경우 권한 변경을 거부합니다.
     *
     * @param userId 권한 변경 대상 사용자 ID
     * @throws LastAdminCannotChangeException 마지막 ADMIN인 경우
     */
    @Transactional(readOnly = true)
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
}
