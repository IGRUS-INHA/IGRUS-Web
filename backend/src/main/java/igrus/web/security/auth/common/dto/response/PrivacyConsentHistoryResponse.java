package igrus.web.security.auth.common.dto.response;

import igrus.web.security.auth.common.domain.PrivacyConsent;

import java.util.List;

public record PrivacyConsentHistoryResponse(
        Long userId,

        List<PrivacyConsentResponse> consents,

        int totalCount
) {

    /**
     * PrivacyConsent 엔티티 목록으로부터 이력 응답 DTO를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param consents 동의 엔티티 목록
     * @return 이력 응답 DTO
     */
    public static PrivacyConsentHistoryResponse from(Long userId, List<PrivacyConsent> consents) {
        List<PrivacyConsentResponse> consentResponses = consents.stream()
                .map(PrivacyConsentResponse::from)
                .toList();
        return new PrivacyConsentHistoryResponse(userId, consentResponses, consents.size());
    }
}
