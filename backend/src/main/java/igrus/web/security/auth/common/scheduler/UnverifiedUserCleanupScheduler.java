package igrus.web.security.auth.common.scheduler;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 미인증 사용자 데이터 정리 스케줄러.
 *
 * <p>매일 새벽 3시에 실행되어 지정된 시간(기본 24시간)이 지난
 * 미인증 이메일 인증 레코드와 관련 사용자 데이터를 정리합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnverifiedUserCleanupScheduler {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PrivacyConsentRepository privacyConsentRepository;

    @Value("${app.cleanup.unverified-user-retention-hours:24}")
    private int retentionHours;

    /**
     * 매일 새벽 3시에 미인증 사용자 데이터를 정리합니다.
     *
     * <p>정리 대상:
     * <ul>
     *   <li>지정 시간이 지난 만료된 미인증 EmailVerification 레코드</li>
     *   <li>이메일 인증을 완료한 적이 없는 사용자와 관련 데이터</li>
     * </ul>
     * </p>
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupUnverifiedUsers() {
        log.info("미인증 사용자 정리 작업 시작");

        Instant cutoffTime = Instant.now().minusSeconds(retentionHours * 3600L);

        // 만료된 미인증 EmailVerification 조회
        List<EmailVerification> expiredVerifications =
            emailVerificationRepository.findByExpiresAtBeforeAndVerifiedFalse(cutoffTime);

        int deletedUserCount = 0;
        int deletedVerificationCount = 0;

        for (EmailVerification verification : expiredVerifications) {
            String email = verification.getEmail();

            // 해당 이메일로 이미 인증 완료된 기록이 있는지 확인
            if (!emailVerificationRepository.existsByEmailAndVerifiedTrue(email)) {
                // 이메일 인증을 완료한 적이 없는 사용자 -> 삭제 대상
                userRepository.findByEmail(email).ifPresent(user -> {
                    // 관련 데이터 삭제 (cascade가 설정되지 않은 경우)
                    privacyConsentRepository.deleteByUserId(user.getId());
                    passwordCredentialRepository.deleteByUserId(user.getId());
                    userRepository.delete(user);
                    log.info("미인증 사용자 삭제: email={}, userId={}", email, user.getId());
                });
                deletedUserCount++;
            }

            // EmailVerification 레코드 삭제
            emailVerificationRepository.delete(verification);
            deletedVerificationCount++;
        }

        log.info("미인증 사용자 정리 작업 완료: 삭제된 사용자 수={}, 삭제된 인증 레코드 수={}",
            deletedUserCount, deletedVerificationCount);
    }
}
