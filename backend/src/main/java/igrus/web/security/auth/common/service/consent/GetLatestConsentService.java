package igrus.web.security.auth.common.service.consent;

import igrus.web.security.auth.common.dto.response.PrivacyConsentResponse;
import igrus.web.security.auth.common.repository.PrivacyConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 사용자의 최신 동의 기록 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetLatestConsentService {

    private final PrivacyConsentRepository privacyConsentRepository;

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
}
