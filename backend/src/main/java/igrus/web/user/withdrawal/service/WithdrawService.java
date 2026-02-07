package igrus.web.user.withdrawal.service;

import igrus.web.security.auth.common.exception.account.AccountSuspendedException;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.withdrawal.domain.WithdrawalLog;
import igrus.web.user.withdrawal.dto.request.WithdrawRequest;
import igrus.web.user.withdrawal.repository.WithdrawalLogRepository;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.event.AccountStatusChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final WithdrawalLogRepository withdrawalLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void withdraw(Long userId, WithdrawRequest request) {
        log.info("회원 탈퇴 요청 - userId: {}", userId);

        // 1. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 2. 정지 상태 확인
        if (user.isSuspended()) {
            log.warn("회원 탈퇴 실패 - 정지된 계정: userId={}", userId);
            throw new AccountSuspendedException();
        }

        // 3. PasswordCredential 조회
        PasswordCredential credential = passwordCredentialRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 4. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            log.warn("회원 탈퇴 실패 - 비밀번호 불일치: userId={}", userId);
            throw new InvalidCredentialsException();
        }

        // 5. 탈퇴 로그 저장
        WithdrawalLog withdrawalLog = WithdrawalLog.create(user, request.reason());
        withdrawalLogRepository.save(withdrawalLog);

        // 6. User 상태 WITHDRAWN + soft delete
        user.withdraw();
        user.delete(userId);

        // 7. PasswordCredential 상태 WITHDRAWN + soft delete
        credential.withdraw();
        credential.delete(userId);

        // 8. RefreshToken 전부 무효화
        refreshTokenRepository.revokeAllByUserId(userId);

        // 9. 감사 이력 이벤트 발행
        eventPublisher.publishEvent(new AccountStatusChangeEvent(
                userId, userId, AccountChangeType.WITHDRAWAL,
                UserStatus.ACTIVE.name(), UserStatus.WITHDRAWN.name(),
                request.reason()
        ));

        log.info("회원 탈퇴 완료 - userId: {}", userId);
    }
}
