package igrus.web.security.auth.password.controller;

import igrus.web.common.exception.ErrorCode;
import igrus.web.common.exception.GlobalExceptionHandler;
import igrus.web.security.auth.common.exception.token.RefreshTokenExpiredException;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.common.exception.token.RefreshTokenTheftException;
import igrus.web.security.auth.common.service.account.CheckReRegistrationEligibilityService;
import igrus.web.security.auth.common.service.account.CheckRecoveryEligibilityService;
import igrus.web.security.auth.common.service.account.RecoverAccountService;
import igrus.web.security.auth.common.service.AccountStatusService;
import igrus.web.security.auth.common.util.CookieUtil;
import igrus.web.security.auth.password.dto.internal.TokenRotationResult;
import igrus.web.security.auth.password.service.reset.RequestPasswordResetService;
import igrus.web.security.auth.password.service.reset.ResetPasswordService;
import igrus.web.security.auth.password.service.reset.ValidateResetTokenService;
import igrus.web.security.auth.password.service.presignup.PreSignupSendCodeService;
import igrus.web.security.auth.password.service.presignup.PreSignupVerifyCodeService;
import igrus.web.security.auth.password.service.signup.CheckDuplicateService;
import igrus.web.security.auth.password.service.signup.SignupService;
import igrus.web.security.auth.password.service.signup.TempStudentIdSignupService;
import igrus.web.security.auth.password.service.auth.LoginService;
import igrus.web.security.auth.password.service.auth.LogoutService;
import igrus.web.security.auth.password.service.auth.RefreshTokenService;
import igrus.web.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("PasswordAuthController 토큰 갱신 테스트")
class PasswordAuthControllerTokenTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private LogoutService logoutService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private SignupService signupService;

    @MockitoBean
    private TempStudentIdSignupService tempStudentIdSignupService;

    @MockitoBean
    private CheckDuplicateService checkDuplicateService;

    @MockitoBean
    private PreSignupSendCodeService preSignupSendCodeService;

    @MockitoBean
    private PreSignupVerifyCodeService preSignupVerifyCodeService;

    @MockitoBean
    private RequestPasswordResetService requestPasswordResetService;

    @MockitoBean
    private ResetPasswordService resetPasswordService;

    @MockitoBean
    private ValidateResetTokenService validateResetTokenService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CheckReRegistrationEligibilityService checkReRegistrationEligibilityService;

    @MockitoBean
    private CheckRecoveryEligibilityService checkRecoveryEligibilityService;

    @MockitoBean
    private RecoverAccountService recoverAccountService;

    @MockitoBean
    private AccountStatusService accountStatusService;

    @MockitoBean
    private CookieUtil cookieUtil;

    private static final String VALID_REFRESH_TOKEN = "valid.refresh.token";

    private static final String EXPIRED_REFRESH_TOKEN = "expired.refresh.token";
    private static final String INVALID_REFRESH_TOKEN = "invalid.refresh.token";
    private static final String FORGED_REFRESH_TOKEN = "forged.malicious.token";
    private static final String REVOKED_REFRESH_TOKEN = "revoked.logout.token";
    private static final long EXPIRES_IN = 3600000L; // 1 hour
    private static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        // Mock for refresh - getRefreshTokenFromCookies returns the token from cookie
        given(cookieUtil.getRefreshTokenFromCookies(any()))
                .willReturn(Optional.of(VALID_REFRESH_TOKEN));

        // Mock for refresh - createRefreshTokenCookie returns a cookie
        given(cookieUtil.createRefreshTokenCookie(anyString(), any(Duration.class)))
                .willReturn(ResponseCookie.from("refreshToken", "new.refresh.token")
                        .httpOnly(true)
                        .path("/api/v1/auth")
                        .build());

        // Mock for theft detection - deleteRefreshTokenCookie returns a delete cookie
        given(cookieUtil.deleteRefreshTokenCookie())
                .willReturn(ResponseCookie.from("refreshToken", "")
                        .httpOnly(true)
                        .path("/api/v1/auth")
                        .maxAge(0)
                        .build());
    }

    @Nested
    @DisplayName("토큰 갱신 성공")
    class TokenRefreshSuccessTest {

        @Test
        @DisplayName("유효한 Refresh Token으로 갱신 시 새 Access Token 반환 [TKN-001]")
        void refreshToken_withValidToken_returns200() throws Exception {
            // given
            TokenRotationResult result = new TokenRotationResult("new.access.token", "new.refresh.token", EXPIRES_IN, REFRESH_TOKEN_VALIDITY);
            given(refreshTokenService.refreshToken(anyString()))
                .willReturn(result);

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .cookie(new Cookie("refreshToken", VALID_REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.expiresIn").value(EXPIRES_IN));
        }

        @Test
        @DisplayName("새 Access Token 유효기간 확인 [TKN-002]")
        void refreshToken_withValidToken_returnsExpiresInGreaterThanZero() throws Exception {
            // given
            TokenRotationResult result = new TokenRotationResult("new.access.token", "new.refresh.token", EXPIRES_IN, REFRESH_TOKEN_VALIDITY);
            given(refreshTokenService.refreshToken(anyString()))
                .willReturn(result);

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .cookie(new Cookie("refreshToken", VALID_REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.expiresIn").value(org.hamcrest.Matchers.greaterThan(0)));
        }

        @Test
        @DisplayName("갱신된 토큰으로 API 호출 가능 [TKN-003]")
        void refreshToken_withValidToken_returnsValidAccessToken() throws Exception {
            // given
            String newAccessToken = "valid.new.access.token.for.api.calls";
            TokenRotationResult result = new TokenRotationResult(newAccessToken, "new.refresh.token", EXPIRES_IN, REFRESH_TOKEN_VALIDITY);
            given(refreshTokenService.refreshToken(anyString()))
                .willReturn(result);

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .cookie(new Cookie("refreshToken", VALID_REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(newAccessToken))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("토큰 갱신 실패")
    class TokenRefreshFailureTest {

        @Test
        @DisplayName("만료된 Refresh Token으로 갱신 시도 시 401 반환 [TKN-010]")
        void refreshToken_withExpiredToken_returns401() throws Exception {
            // given
            given(refreshTokenService.refreshToken(anyString()))
                .willThrow(new RefreshTokenExpiredException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .cookie(new Cookie("refreshToken", EXPIRED_REFRESH_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_EXPIRED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.REFRESH_TOKEN_EXPIRED.getMessage()));
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token으로 갱신 시도 시 401 반환 [TKN-011]")
        void refreshToken_withInvalidToken_returns401() throws Exception {
            // given
            given(refreshTokenService.refreshToken(anyString()))
                .willThrow(new RefreshTokenInvalidException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .cookie(new Cookie("refreshToken", INVALID_REFRESH_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.REFRESH_TOKEN_INVALID.getMessage()));
        }

        @Test
        @DisplayName("위조된 Refresh Token으로 갱신 시도 시 401 반환 [TKN-012]")
        void refreshToken_withForgedToken_returns401() throws Exception {
            // given
            given(refreshTokenService.refreshToken(anyString()))
                .willThrow(new RefreshTokenInvalidException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .cookie(new Cookie("refreshToken", FORGED_REFRESH_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.REFRESH_TOKEN_INVALID.getMessage()));
        }

        @Test
        @DisplayName("쿠키 없이 갱신 시도 시 401 반환 [TKN-013]")
        void refreshToken_withNoCookie_returns401() throws Exception {
            // given - 쿠키 없이 요청: getRefreshTokenFromCookies가 빈 Optional 반환
            given(cookieUtil.getRefreshTokenFromCookies(any()))
                    .willReturn(Optional.empty());

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_INVALID.getCode()));
        }

        @Test
        @DisplayName("탈취 감지 시 401 반환 및 쿠키 삭제 [TKN-014]")
        void refreshToken_withTheftDetected_returns401AndDeletesCookie() throws Exception {
            // given
            given(refreshTokenService.refreshToken(anyString()))
                .willThrow(new RefreshTokenTheftException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .cookie(new Cookie("refreshToken", REVOKED_REFRESH_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_THEFT_DETECTED.getCode()))
                .andExpect(header().exists("Set-Cookie"));

            verify(cookieUtil).deleteRefreshTokenCookie();
        }

        @Test
        @DisplayName("만료된 Refresh Token 시 401 반환 및 쿠키 삭제 [TKN-015]")
        void refreshToken_withExpiredToken_returns401AndDeletesCookie() throws Exception {
            // given
            given(refreshTokenService.refreshToken(anyString()))
                .willThrow(new RefreshTokenExpiredException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .cookie(new Cookie("refreshToken", EXPIRED_REFRESH_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_EXPIRED.getCode()))
                .andExpect(header().exists("Set-Cookie"));

            verify(cookieUtil).deleteRefreshTokenCookie();
        }
    }

    @Nested
    @DisplayName("토큰 갱신 - 쿠키 동작")
    class TokenRefreshCookieTest {

        @Test
        @DisplayName("정상 갱신 시 Set-Cookie 헤더에 새 Refresh Token 포함 [TKN-005]")
        void refreshToken_withValidToken_setsCookieHeader() throws Exception {
            // given
            TokenRotationResult result = new TokenRotationResult("new.access.token", "new.refresh.token", EXPIRES_IN, REFRESH_TOKEN_VALIDITY);
            given(refreshTokenService.refreshToken(anyString()))
                .willReturn(result);

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .cookie(new Cookie("refreshToken", VALID_REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));

            verify(cookieUtil).createRefreshTokenCookie(anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("Grace Period 내 응답 시 Set-Cookie 미포함 [TKN-033]")
        void refreshToken_withinGracePeriod_doesNotSetCookie() throws Exception {
            // given - Grace Period: newRefreshToken이 null
            TokenRotationResult result = new TokenRotationResult("new.access.token", null, EXPIRES_IN, 0);
            given(refreshTokenService.refreshToken(anyString()))
                .willReturn(result);

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .cookie(new Cookie("refreshToken", VALID_REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"));

            verify(cookieUtil, never()).createRefreshTokenCookie(anyString(), any(Duration.class));
        }
    }
}
