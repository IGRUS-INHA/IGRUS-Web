package igrus.web.security.auth.common.dto.response;

import igrus.web.security.auth.common.domain.PrivacyConsent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "개인정보 동의 응답")
public record PrivacyConsentResponse(
        @Schema(description = "동의 ID", example = "1")
        Long id,

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "동의 여부", example = "true")
        boolean consentGiven,

        @Schema(description = "동의 일시", example = "2024-01-15T10:30:00Z")
        Instant consentDate,

        @Schema(description = "동의한 개인정보 처리방침 버전", example = "1.0")
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
