package igrus.web.security.auth.common.service.consent;

import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 사용자가 현재 정책 버전에 대해 재동의가 필요한지 확인하는 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NeedsReConsentService {

    private final PrivacyConsentRepository privacyConsentRepository;

    /**
     * 사용자가 현재 정책 버전에 대해 재동의가 필요한지 확인합니다.
     *
     * <p>최신 동의 기록의 정책 버전이 현재 정책 버전과 다르면 재동의가 필요합니다.</p>
     *
     * @param userId 사용자 ID
     * @param currentPolicyVersion 현재 정책 버전
     * @return 재동의 필요 여부
     */
    @Transactional(readOnly = true)
    public boolean needsReConsent(Long userId, String currentPolicyVersion) {
        Optional<PrivacyConsent> latestConsent = privacyConsentRepository
                .findFirstByUserIdOrderByConsentDateDesc(userId);

        if (latestConsent.isEmpty()) {
            log.debug("사용자 동의 기록 없음, 재동의 필요: userId={}", userId);
            return true;
        }

        boolean needsReConsent = !latestConsent.get().getPolicyVersion().equals(currentPolicyVersion);
        if (needsReConsent) {
            log.debug("정책 버전 불일치, 재동의 필요: userId={}, userVersion={}, currentVersion={}",
                    userId, latestConsent.get().getPolicyVersion(), currentPolicyVersion);
        }
        return needsReConsent;
    }
}
