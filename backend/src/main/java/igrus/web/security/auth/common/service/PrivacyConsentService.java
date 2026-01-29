package igrus.web.security.auth.common.service;

import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.dto.response.PrivacyConsentHistoryResponse;
import igrus.web.security.auth.common.dto.response.PrivacyConsentResponse;
import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 개인정보 동의 서비스.
 *
 * <p>개인정보 동의 이력 관리 및 재동의 필요 여부를 판단합니다.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PrivacyConsentService {

    private final PrivacyConsentRepository privacyConsentRepository;

    /**
     * 사용자의 동의 이력을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 동의 이력 응답
     */
    @Transactional(readOnly = true)
    public PrivacyConsentHistoryResponse getConsentHistory(Long userId) {
        List<PrivacyConsent> consents = privacyConsentRepository.findByUserIdOrderByConsentDateDesc(userId);
        log.debug("사용자 동의 이력 조회: userId={}, count={}", userId, consents.size());
        return PrivacyConsentHistoryResponse.from(userId, consents);
    }

    /**
     * 사용자의 최신 동의 기록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 최신 동의 응답 (Optional)
     */
    @Transactional(readOnly = true)
    public Optional<PrivacyConsentResponse> getLatestConsent(Long userId) {
        return privacyConsentRepository.findFirstByUserIdOrderByConsentDateDesc(userId)
                .map(PrivacyConsentResponse::from);
    }

    /**
     * 사용자가 특정 버전의 약관에 동의했는지 확인합니다.
     *
     * @param userId 사용자 ID
     * @param policyVersion 정책 버전
     * @return 해당 버전 동의 여부
     */
    @Transactional(readOnly = true)
    public boolean hasConsentedToVersion(Long userId, String policyVersion) {
        return privacyConsentRepository.existsByUserIdAndPolicyVersion(userId, policyVersion);
    }

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

    /**
     * 사용자가 동의한 기록이 있는지 확인합니다.
     *
     * @param userId 사용자 ID
     * @return 동의 기록 존재 여부
     */
    @Transactional(readOnly = true)
    public boolean hasAnyConsent(Long userId) {
        return privacyConsentRepository.existsByUserIdAndConsentGivenTrue(userId);
    }
}
