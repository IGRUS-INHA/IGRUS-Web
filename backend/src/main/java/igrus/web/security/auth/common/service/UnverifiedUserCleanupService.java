package igrus.web.security.auth.common.service;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 미인증 사용자 정리 서비스.
 *
 * <p>지정된 시간이 지난 미인증 이메일 인증 레코드와
 * 관련 사용자 데이터를 정리하는 비즈니스 로직을 담당합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UnverifiedUserCleanupService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PrivacyConsentRepository privacyConsentRepository;

    /**
     * 미인증 사용자 데이터를 정리합니다.
     *
     * <p>정리 대상:
     * <ul>
     *   <li>지정 시간이 지난 만료된 미인증 EmailVerification 레코드</li>
     *   <li>이메일 인증을 완료한 적이 없는 사용자와 관련 데이터</li>
     * </ul>
     * </p>
     *
     * @param retentionHours 보존 시간 (시간 단위)
     * @return 정리 결과 (삭제된 사용자 수, 삭제된 인증 레코드 수)
     */
    public CleanupResult cleanup(int retentionHours) {
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
                if (deleteUserByEmail(email)) {
                    deletedUserCount++;
                }
            }

            // EmailVerification 레코드 삭제
            emailVerificationRepository.delete(verification);
            deletedVerificationCount++;
        }

        log.info("미인증 사용자 정리 작업 완료: 삭제된 사용자 수={}, 삭제된 인증 레코드 수={}",
            deletedUserCount, deletedVerificationCount);

        return new CleanupResult(deletedUserCount, deletedVerificationCount);
    }

    /**
     * 이메일로 사용자와 관련 데이터를 삭제합니다.
     *
     * @param email 삭제할 사용자의 이메일
     * @return 삭제 여부 (사용자가 존재하여 삭제했으면 true)
     */
    private boolean deleteUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .map(user -> {
                // 관련 데이터 삭제 (cascade가 설정되지 않은 경우)
                privacyConsentRepository.deleteByUserId(user.getId());
                passwordCredentialRepository.deleteByUserId(user.getId());
                userRepository.delete(user);
                log.info("미인증 사용자 삭제: email={}, userId={}", email, user.getId());
                return true;
            })
            .orElse(false);
    }

    /**
     * 정리 작업 결과를 담는 레코드.
     *
     * @param deletedUserCount 삭제된 사용자 수
     * @param deletedVerificationCount 삭제된 인증 레코드 수
     */
    public record CleanupResult(int deletedUserCount, int deletedVerificationCount) {}
}
