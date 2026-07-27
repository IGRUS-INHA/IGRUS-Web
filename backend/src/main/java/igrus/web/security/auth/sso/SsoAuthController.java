package igrus.web.security.auth.sso;

import igrus.web.common.util.ServletContextUtil;
import igrus.web.generated.api.SsoAuthenticationApi;
import igrus.web.generated.model.ApiSsoTokenRequest;
import igrus.web.generated.model.ApiSsoTokenResponse;
import igrus.web.security.auth.common.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class SsoAuthController implements SsoAuthenticationApi {

    private final SsoService ssoService;
    private final CookieUtil cookieUtil;

    @Override
    public ResponseEntity<Void> ssoAuthorize(URI redirectUri) {
        String redirectUriValue = redirectUri.toString();
        ssoService.validateRedirectUri(redirectUriValue);

        HttpServletRequest request = ServletContextUtil.getCurrentRequest();
        String location = cookieUtil.getRefreshTokenFromCookies(request)
                .flatMap(ssoService::issueCodeForRefreshToken)
                .map(code -> appendQueryParam(redirectUriValue, "sso_code", code))
                .orElseGet(() -> appendQueryParam(redirectUriValue, "sso", "none"));

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    @Override
    public ResponseEntity<ApiSsoTokenResponse> ssoIssueToken(ApiSsoTokenRequest apiSsoTokenRequest) {
        SsoTokenResult result = ssoService.exchangeCode(apiSsoTokenRequest.getCode());
        return ResponseEntity.ok(new ApiSsoTokenResponse()
                .accessToken(result.accessToken())
                .refreshToken(result.refreshToken())
                .expiresIn(result.accessTokenValidity())
                .refreshExpiresIn(result.refreshTokenValidity()));
    }

    private static String appendQueryParam(String uri, String name, String value) {
        String separator = uri.contains("?") ? "&" : "?";
        return uri + separator + name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
