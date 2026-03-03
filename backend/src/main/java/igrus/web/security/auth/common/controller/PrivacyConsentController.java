package igrus.web.security.auth.common.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.PrivacyConsentApi;
import igrus.web.generated.model.CheckNeedsReConsent200Response;
import igrus.web.generated.model.GetConsentHistory200Response;
import igrus.web.generated.model.GetLatestConsent200Response;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.security.auth.common.dto.response.PrivacyConsentHistoryResponse;
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
    public ResponseEntity<GetConsentHistory200Response> getConsentHistory() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        PrivacyConsentHistoryResponse internal = getConsentHistoryService.getConsentHistory(user.userId());

        GetConsentHistory200Response response = new GetConsentHistory200Response()
                .userId(internal.userId())
                .consents(internal.consents().stream()
                        .map(c -> new GetLatestConsent200Response()
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
    public ResponseEntity<GetLatestConsent200Response> getLatestConsent() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        return getLatestConsentService.getLatestConsent(user.userId())
                .map(c -> ResponseEntity.ok(new GetLatestConsent200Response()
                        .id(c.id())
                        .userId(c.userId())
                        .consentGiven(c.consentGiven())
                        .consentDate(c.consentDate())
                        .policyVersion(c.policyVersion())))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<CheckNeedsReConsent200Response> checkHasConsent() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        boolean hasConsent = hasAnyConsentService.hasAnyConsent(user.userId());
        return ResponseEntity.ok(new CheckNeedsReConsent200Response().result(hasConsent));
    }

    @Override
    public ResponseEntity<CheckNeedsReConsent200Response> checkConsentedToVersion(String version) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        boolean consented = hasConsentedToVersionService.hasConsentedToVersion(user.userId(), version);
        return ResponseEntity.ok(new CheckNeedsReConsent200Response().result(consented));
    }

    @Override
    public ResponseEntity<CheckNeedsReConsent200Response> checkNeedsReConsent(String currentVersion) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        boolean needsReConsent = needsReConsentService.needsReConsent(user.userId(), currentVersion);
        return ResponseEntity.ok(new CheckNeedsReConsent200Response().result(needsReConsent));
    }
}
