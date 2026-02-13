package igrus.web.admin.user.service;

import igrus.web.admin.user.exception.UserNotPendingVerificationException;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.event.AccountStatusChangeEvent;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ForceActivateService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void forceActivate(Long targetUserId, Long currentUserId) {
        log.info("강제 활성화 요청 - targetUserId: {}, performedBy: {}", targetUserId, currentUserId);

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        if (!targetUser.isPendingVerification()) {
            throw new UserNotPendingVerificationException();
        }

        String previousStatus = targetUser.getStatus().name();

        targetUser.verifyEmail();

        passwordCredentialRepository.findByUserId(targetUserId)
                .ifPresent(credential -> credential.verifyEmail());

        emailVerificationRepository.deleteByEmail(targetUser.getEmail());

        eventPublisher.publishEvent(new AccountStatusChangeEvent(
                targetUserId, currentUserId, AccountChangeType.FORCE_ACTIVATION,
                previousStatus, UserStatus.ACTIVE.name(),
                "관리자에 의한 강제 활성화 (이메일 인증 우회)"
        ));

        log.info("강제 활성화 완료 - targetUserId: {}, performedBy: {}", targetUserId, currentUserId);
    }
}
