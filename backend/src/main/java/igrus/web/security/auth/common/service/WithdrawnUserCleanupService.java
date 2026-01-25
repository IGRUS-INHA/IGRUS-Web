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
import java.util.UUID;

/**
 * 탈퇴 사용자 개인정보 정리 서비스.
 *
 * <p>탈퇴 후 복구 가능 기간(5일)이 경과한 사용자의 개인정보를 영구 삭제합니다.
 * 개인정보보호법 준수 및 데이터 최소화 원칙을 따릅니다.</p>
 *
 * <h3>완전 삭제 대상:</h3>
 * <ul>
 *   <li>비밀번호 자격 증명 (PasswordCredential)</li>
 *   <li>개인정보 동의 기록 (PrivacyConsent)</li>
 *   <li>이메일 인증 기록 (EmailVerification)</li>
 *   <li>Refresh Token</li>
 * </ul>
 *
 * <h3>익명화 처리 대상:</h3>
 * <ul>
 *   <li>이름: "탈퇴회원_" + 랜덤 해시</li>
 *   <li>이메일: "deleted_" + 랜덤 해시 + "@deleted.local"</li>
 *   <li>학번: "DELETED_" + 사용자 ID</li>
 *   <li>전화번호, 학과, 가입동기: null</li>
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
     * 탈퇴 후 복구 기간이 만료된 사용자의 개인정보를 익명화합니다.
     *
     * @return 처리된 사용자 수
     */
    public int anonymizeExpiredWithdrawnUsers() {
        Instant cutoffTime = Instant.now().minus(RECOVERY_PERIOD_DAYS, ChronoUnit.DAYS);

        List<User> usersToAnonymize = userRepository.findWithdrawnUsersBeforeAndNotAnonymized(cutoffTime);

        int count = 0;
        for (User user : usersToAnonymize) {
            anonymizeUser(user);
            count++;
        }

        return count;
    }

    /**
     * 단일 사용자의 개인정보를 익명화합니다.
     *
     * @param user 익명화할 사용자
     */
    private void anonymizeUser(User user) {
        Long userId = user.getId();
        String email = user.getEmail();
        String anonymousHash = generateAnonymousHash();

        // 1. 연관 데이터 삭제 (hard delete - soft deleted User의 연관 데이터도 삭제)
        passwordCredentialRepository.hardDeleteByUserId(userId);
        privacyConsentRepository.hardDeleteByUserId(userId);
        emailVerificationRepository.deleteByEmail(email);
        refreshTokenRepository.hardDeleteByUserId(userId);

        // 2. 사용자 정보 익명화
        user.anonymize(anonymousHash);
        userRepository.save(user);

        log.info("탈퇴 사용자 익명화 완료: userId={}", userId);
    }

    /**
     * 익명화에 사용할 랜덤 해시를 생성합니다.
     *
     * @return 8자리 랜덤 해시
     */
    private String generateAnonymousHash() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
