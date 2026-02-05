package igrus.web.security.auth.common.service.consent;

import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.dto.response.PrivacyConsentHistoryResponse;
import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자의 동의 이력 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetConsentHistoryService {

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
}
