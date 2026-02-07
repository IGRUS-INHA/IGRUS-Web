package igrus.web.admin.user.service;

import igrus.web.admin.user.dto.ChangeUserStatusRequest;
import igrus.web.admin.user.exception.SelfStatusChangeException;
import igrus.web.common.exception.ErrorCode;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserSuspension;
import igrus.web.user.exception.InvalidSuspensionException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.repository.UserSuspensionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangeUserStatusService {

    private final UserRepository userRepository;
    private final UserSuspensionRepository userSuspensionRepository;

    public void changeUserStatus(Long targetUserId, ChangeUserStatusRequest request, Long currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            throw new SelfStatusChangeException();
        }

        switch (request.action()) {
            case SUSPEND -> suspend(targetUserId, request, currentUserId);
            case LIFT -> lift(targetUserId, currentUserId);
        }
    }

    private void suspend(Long targetUserId, ChangeUserStatusRequest request, Long currentUserId) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw InvalidSuspensionException.reasonRequired();
        }
        if (request.suspendedUntil() == null) {
            throw new InvalidSuspensionException(ErrorCode.SUSPENSION_INVALID_PERIOD);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        if (targetUser.isSuspended()) {
            throw new InvalidSuspensionException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        targetUser.suspend();

        UserSuspension suspension = UserSuspension.create(
                targetUser, request.reason(), request.suspendedUntil(), currentUserId);
        userSuspensionRepository.save(suspension);
    }

    private void lift(Long targetUserId, Long currentUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        UserSuspension suspension = userSuspensionRepository
                .findActiveByUserId(targetUserId, Instant.now())
                .orElseThrow(InvalidSuspensionException::alreadyLifted);

        suspension.lift(currentUserId);
        targetUser.activate();
    }
}
