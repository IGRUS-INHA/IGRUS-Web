package igrus.web.security.auth.common.service.consent;

import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자가 동의한 기록이 있는지 확인하는 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HasAnyConsentService {

    private final PrivacyConsentRepository privacyConsentRepository;

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
