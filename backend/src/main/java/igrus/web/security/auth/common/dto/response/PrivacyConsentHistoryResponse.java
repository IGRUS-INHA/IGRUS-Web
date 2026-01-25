package igrus.web.security.auth.common.dto.response;

import igrus.web.security.auth.common.domain.PrivacyConsent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "개인정보 동의 이력 응답")
public record PrivacyConsentHistoryResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "동의 이력 목록 (최신순)")
        List<PrivacyConsentResponse> consents,

        @Schema(description = "총 동의 횟수", example = "3")
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
