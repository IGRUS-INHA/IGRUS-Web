package igrus.web.security.auth.common.service.consent;

import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자가 특정 버전의 약관에 동의했는지 확인하는 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HasConsentedToVersionService {

    private final PrivacyConsentRepository privacyConsentRepository;

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
}
