package igrus.web.admin.user.service;

import igrus.web.admin.user.exception.SelfRoleChangeException;
import igrus.web.security.auth.approval.exception.LastAdminCannotChangeException;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangeUserRoleService {

    private final UserRepository userRepository;

    public void changeUserRole(Long targetUserId, UserRole newRole, Long currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            throw new SelfRoleChangeException();
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        if (targetUser.isAdmin() && newRole != UserRole.ADMIN) {
            long adminCount = userRepository.countByRole(UserRole.ADMIN);
            if (adminCount <= 1) {
                throw new LastAdminCannotChangeException();
            }
        }

        targetUser.changeRole(newRole);
    }
}
