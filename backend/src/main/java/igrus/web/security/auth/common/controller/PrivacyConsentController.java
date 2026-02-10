package igrus.web.security.auth.common.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.security.auth.common.dto.response.ConsentCheckResponse;
import igrus.web.security.auth.common.dto.response.PrivacyConsentHistoryResponse;
import igrus.web.security.auth.common.dto.response.PrivacyConsentResponse;
import igrus.web.security.auth.common.service.consent.GetConsentHistoryService;
import igrus.web.security.auth.common.service.consent.GetLatestConsentService;
import igrus.web.security.auth.common.service.consent.HasAnyConsentService;
import igrus.web.security.auth.common.service.consent.HasConsentedToVersionService;
import igrus.web.security.auth.common.service.consent.NeedsReConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Privacy Consent", description = "개인정보 동의 관리 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/v1/privacy/consent")
@RequiredArgsConstructor
public class PrivacyConsentController {

    private final GetConsentHistoryService getConsentHistoryService;
    private final GetLatestConsentService getLatestConsentService;
    private final HasAnyConsentService hasAnyConsentService;
    private final HasConsentedToVersionService hasConsentedToVersionService;
    private final NeedsReConsentService needsReConsentService;

    @Operation(summary = "동의 이력 조회", description = "사용자의 개인정보 동의 이력을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/history")
    public ResponseEntity<PrivacyConsentHistoryResponse> getConsentHistory(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        PrivacyConsentHistoryResponse response = getConsentHistoryService.getConsentHistory(user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "최신 동의 기록 조회", description = "사용자의 최신 개인정보 동의 기록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "동의 기록 없음")
    })
    @GetMapping("/latest")
    public ResponseEntity<PrivacyConsentResponse> getLatestConsent(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return getLatestConsentService.getLatestConsent(user.userId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "동의 기록 존재 확인", description = "사용자가 동의한 기록이 있는지 확인합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/check")
    public ResponseEntity<ConsentCheckResponse> checkHasConsent(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        boolean hasConsent = hasAnyConsentService.hasAnyConsent(user.userId());
        return ResponseEntity.ok(new ConsentCheckResponse(hasConsent));
    }

    @Operation(summary = "특정 버전 동의 확인", description = "사용자가 특정 버전의 정책에 동의했는지 확인합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/check-version")
    public ResponseEntity<ConsentCheckResponse> checkConsentedToVersion(
            @Parameter(description = "확인할 정책 버전", example = "v1.0", required = true)
            @RequestParam String version,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        boolean consented = hasConsentedToVersionService.hasConsentedToVersion(user.userId(), version);
        return ResponseEntity.ok(new ConsentCheckResponse(consented));
    }

    @Operation(summary = "재동의 필요 여부 확인", description = "사용자가 현재 정책 버전에 재동의가 필요한지 확인합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/needs-reconsent")
    public ResponseEntity<ConsentCheckResponse> checkNeedsReConsent(
            @Parameter(description = "현재 정책 버전", example = "v2.0", required = true)
            @RequestParam String currentVersion,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        boolean needsReConsent = needsReConsentService.needsReConsent(user.userId(), currentVersion);
        return ResponseEntity.ok(new ConsentCheckResponse(needsReConsent));
    }
}
