package igrus.web.security.auth.common.dto.response;

import igrus.web.security.auth.common.domain.PrivacyConsent;

import java.time.Instant;

public record PrivacyConsentResponse(
        Long id,

        Long userId,

        boolean consentGiven,

        Instant consentDate,

        String policyVersion
) {

    /**
     * PrivacyConsent 엔티티로부터 응답 DTO를 생성합니다.
     *
     * @param consent 동의 엔티티
     * @return 응답 DTO
     */
    public static PrivacyConsentResponse from(PrivacyConsent consent) {
        return new PrivacyConsentResponse(
                consent.getId(),
                consent.getUser().getId(),
                consent.isConsentGiven(),
                consent.getConsentDate(),
                consent.getPolicyVersion()
        );
    }
}
