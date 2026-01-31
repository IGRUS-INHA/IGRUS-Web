package igrus.web.security.auth.common.service;

import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 탈퇴 사용자 인증 데이터 정리 서비스.
 *
 * <p>탈퇴 후 복구 가능 기간(5일)이 경과한 사용자의 인증 데이터를 영구 삭제합니다.</p>
 *
 * <h3>완전 삭제 대상:</h3>
 * <ul>
 *   <li>비밀번호 자격 증명 (PasswordCredential)</li>
 *   <li>개인정보 동의 기록 (PrivacyConsent)</li>
 *   <li>이메일 인증 기록 (EmailVerification)</li>
 *   <li>Refresh Token</li>
 * </ul>
 *
 * @see igrus.web.security.auth.common.scheduler.WithdrawnUserCleanupScheduler
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawnUserCleanupService {

    private static final int RECOVERY_PERIOD_DAYS = 5;

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PrivacyConsentRepository privacyConsentRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 탈퇴 후 복구 기간이 만료된 사용자의 인증 데이터를 정리합니다.
     *
     * @return 처리된 사용자 수
     */
    public int cleanupExpiredWithdrawnUsers() {
        Instant cutoffTime = Instant.now().minus(RECOVERY_PERIOD_DAYS, ChronoUnit.DAYS);

        List<User> usersToCleanup = userRepository.findWithdrawnUsersBefore(cutoffTime);

        int count = 0;
        for (User user : usersToCleanup) {
            cleanupUser(user);
            count++;
        }

        return count;
    }

    /**
     * 단일 사용자의 인증 데이터를 정리합니다.
     *
     * @param user 정리할 사용자
     */
    private void cleanupUser(User user) {
        Long userId = user.getId();
        String email = user.getEmail();

        // 연관 인증 데이터 삭제 (hard delete)
        passwordCredentialRepository.hardDeleteByUserId(userId);
        privacyConsentRepository.hardDeleteByUserId(userId);
        emailVerificationRepository.deleteByEmail(email);
        refreshTokenRepository.hardDeleteByUserId(userId);

        // 재가입 시 unique 제약조건 충돌 방지를 위한 익명화
        user.anonymizeForCleanup();

        log.info("탈퇴 사용자 인증 데이터 정리 완료: userId={}", userId);
    }
}
