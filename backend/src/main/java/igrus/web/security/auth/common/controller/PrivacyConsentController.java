package igrus.web.security.auth.common.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.security.auth.common.dto.response.PrivacyConsentHistoryResponse;
import igrus.web.generated.api.PrivacyConsentApi;
import igrus.web.generated.model.ApiConsentCheckResponse;
import igrus.web.generated.model.ApiPrivacyConsentHistoryResponse;
import igrus.web.generated.model.ApiPrivacyConsentResponse;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.security.auth.common.service.consent.GetConsentHistoryService;
import igrus.web.security.auth.common.service.consent.GetLatestConsentService;
import igrus.web.security.auth.common.service.consent.HasAnyConsentService;
import igrus.web.security.auth.common.service.consent.HasConsentedToVersionService;
import igrus.web.security.auth.common.service.consent.NeedsReConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PrivacyConsentController implements PrivacyConsentApi {

    private final GetConsentHistoryService getConsentHistoryService;
    private final GetLatestConsentService getLatestConsentService;
    private final HasAnyConsentService hasAnyConsentService;
    private final HasConsentedToVersionService hasConsentedToVersionService;
    private final NeedsReConsentService needsReConsentService;

    @Override
    public ResponseEntity<ApiPrivacyConsentHistoryResponse> getConsentHistory() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        PrivacyConsentHistoryResponse internal = getConsentHistoryService.getConsentHistory(user.userId());

        ApiPrivacyConsentHistoryResponse response = new ApiPrivacyConsentHistoryResponse()
                .userId(internal.userId())
                .consents(internal.consents().stream()
                        .map(c -> new ApiPrivacyConsentResponse()
                                .id(c.id())
                                .userId(c.userId())
                                .consentGiven(c.consentGiven())
                                .consentDate(c.consentDate())
                                .policyVersion(c.policyVersion()))
                        .toList())
                .totalCount(internal.totalCount());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ApiPrivacyConsentResponse> getLatestConsent() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        return getLatestConsentService.getLatestConsent(user.userId())
                .map(c -> ResponseEntity.ok(new ApiPrivacyConsentResponse()
                        .id(c.id())
                        .userId(c.userId())
                        .consentGiven(c.consentGiven())
                        .consentDate(c.consentDate())
                        .policyVersion(c.policyVersion())))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<ApiConsentCheckResponse> checkHasConsent() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        boolean hasConsent = hasAnyConsentService.hasAnyConsent(user.userId());
        return ResponseEntity.ok(new ApiConsentCheckResponse().result(hasConsent));
    }

    @Override
    public ResponseEntity<ApiConsentCheckResponse> checkConsentedToVersion(String version) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        boolean consented = hasConsentedToVersionService.hasConsentedToVersion(user.userId(), version);
        return ResponseEntity.ok(new ApiConsentCheckResponse().result(consented));
    }

    @Override
    public ResponseEntity<ApiConsentCheckResponse> checkNeedsReConsent(String currentVersion) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        boolean needsReConsent = needsReConsentService.needsReConsent(user.userId(), currentVersion);
        return ResponseEntity.ok(new ApiConsentCheckResponse().result(needsReConsent));
    }
}
