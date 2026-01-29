package igrus.web.security.auth.password.controller;

import igrus.web.common.exception.ErrorCode;
import igrus.web.common.exception.GlobalExceptionHandler;
import igrus.web.security.auth.common.exception.account.AccountSuspendedException;
import igrus.web.security.auth.common.exception.account.AccountWithdrawnException;
import igrus.web.security.auth.common.exception.email.EmailNotVerifiedException;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.common.service.AccountRecoveryService;
import igrus.web.security.auth.common.service.AccountStatusService;
import igrus.web.security.auth.common.util.CookieUtil;
import igrus.web.security.auth.password.controller.fixture.PasswordAuthTestFixture;
import igrus.web.security.auth.password.dto.internal.LoginResult;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.auth.password.service.PasswordResetService;
import igrus.web.security.auth.password.service.PasswordSignupService;
import igrus.web.security.config.ApiSecurityConfig;
import igrus.web.security.config.SecurityConfigUtil;
import igrus.web.security.jwt.JwtAuthenticationFilter;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ApiSecurityConfig.class, SecurityConfigUtil.class, JwtAuthenticationFilter.class})
@DisplayName("PasswordAuthController 로그인/로그아웃 테스트")
class PasswordAuthControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PasswordAuthService passwordAuthService;

    @MockitoBean
    private PasswordSignupService passwordSignupService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private SecurityConfigUtil securityConfigUtil;

    @MockitoBean
    private AccountRecoveryService accountRecoveryService;

    @MockitoBean
    private AccountStatusService accountStatusService;

    @MockitoBean
    private CookieUtil cookieUtil;

    private static final String LOGIN_URL = "/api/v1/auth/password/login";
    private static final String LOGOUT_URL = "/api/v1/auth/password/logout";

    @BeforeEach
    void setUp() {
        // Mock for login - createRefreshTokenCookie
        given(cookieUtil.createRefreshTokenCookie(anyString(), any(Duration.class)))
                .willReturn(ResponseCookie.from("refreshToken", "test-token")
                        .httpOnly(true)
                        .path("/api/v1/auth")
                        .build());

        // Mock for logout - getRefreshTokenFromCookies and deleteRefreshTokenCookie
        given(cookieUtil.getRefreshTokenFromCookies(any()))
                .willReturn(Optional.of(PasswordAuthTestFixture.VALID_REFRESH_TOKEN));
        given(cookieUtil.deleteRefreshTokenCookie())
                .willReturn(ResponseCookie.from("refreshToken", "")
                        .httpOnly(true)
                        .path("/api/v1/auth")
                        .maxAge(0)
                        .build());
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {

        @Nested
        @DisplayName("로그인 성공")
        class LoginSuccessTest {

            @Test
            @DisplayName("준회원 로그인 성공 [LOG-001]")
            void login_withAssociateRole_returns200() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.validLoginRequest();
                LoginResult result = PasswordAuthTestFixture.associateLoginResult();

                given(passwordAuthService.login(any(PasswordLoginRequest.class), anyString(), any()))
                        .willReturn(result);

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").value(PasswordAuthTestFixture.VALID_ACCESS_TOKEN))
                        .andExpect(jsonPath("$.userId").value(PasswordAuthTestFixture.VALID_USER_ID))
                        .andExpect(jsonPath("$.studentId").value(PasswordAuthTestFixture.VALID_STUDENT_ID))
                        .andExpect(jsonPath("$.name").value(PasswordAuthTestFixture.VALID_NAME))
                        .andExpect(jsonPath("$.role").value(UserRole.ASSOCIATE.name()))
                        .andExpect(jsonPath("$.expiresIn").value(PasswordAuthTestFixture.VALID_EXPIRES_IN));
            }

            @Test
            @DisplayName("정회원 로그인 - MEMBER 반환 [LOG-002]")
            void login_withMemberRole_returnsMemberRole() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.validLoginRequest();
                LoginResult result = PasswordAuthTestFixture.memberLoginResult();

                given(passwordAuthService.login(any(PasswordLoginRequest.class), anyString(), any()))
                        .willReturn(result);

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.role").value(UserRole.MEMBER.name()));
            }

            @Test
            @DisplayName("운영진 로그인 - OPERATOR 반환 [LOG-003]")
            void login_withOperatorRole_returnsOperatorRole() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.validLoginRequest();
                LoginResult result = PasswordAuthTestFixture.operatorLoginResult();

                given(passwordAuthService.login(any(PasswordLoginRequest.class), anyString(), any()))
                        .willReturn(result);

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.role").value(UserRole.OPERATOR.name()));
            }

            @Test
            @DisplayName("관리자 로그인 - ADMIN 반환 [LOG-004]")
            void login_withAdminRole_returnsAdminRole() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.validLoginRequest();
                LoginResult result = PasswordAuthTestFixture.adminLoginResult();

                given(passwordAuthService.login(any(PasswordLoginRequest.class), anyString(), any()))
                        .willReturn(result);

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.role").value(UserRole.ADMIN.name()));
            }

            @Test
            @DisplayName("로그인 시 사용자 역할 정보 반환 [LOG-007]")
            void login_success_returnsUserRoleInfo() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.validLoginRequest();
                LoginResult result = PasswordAuthTestFixture.memberLoginResult();

                given(passwordAuthService.login(any(PasswordLoginRequest.class), anyString(), any()))
                        .willReturn(result);

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.role").exists())
                        .andExpect(jsonPath("$.userId").exists())
                        .andExpect(jsonPath("$.studentId").exists())
                        .andExpect(jsonPath("$.name").exists());
            }
        }

        @Nested
        @DisplayName("로그인 실패 - 인증 오류")
        class LoginAuthenticationFailureTest {

            @Test
            @DisplayName("잘못된 학번 - 401 Unauthorized [LOG-010]")
            void login_withInvalidStudentId_returns401() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.loginRequestWithInvalidStudentId();

                given(passwordAuthService.login(any(PasswordLoginRequest.class), anyString(), any()))
                        .willThrow(new InvalidCredentialsException());

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()))
                        .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_CREDENTIALS.getMessage()));
            }

            @Test
            @DisplayName("잘못된 비밀번호 - 401 Unauthorized [LOG-011]")
            void login_withInvalidPassword_returns401() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.loginRequestWithInvalidPassword();

                given(passwordAuthService.login(any(PasswordLoginRequest.class), anyString(), any()))
                        .willThrow(new InvalidCredentialsException());

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()))
                        .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_CREDENTIALS.getMessage()));
            }

            @Test
            @DisplayName("이메일 미인증 - 401 Unauthorized [LOG-012]")
            void login_withUnverifiedEmail_returns401() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.validLoginRequest();

                given(passwordAuthService.login(any(PasswordLoginRequest.class), anyString(), any()))
                        .willThrow(new EmailNotVerifiedException());

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_NOT_VERIFIED.getCode()))
                        .andExpect(jsonPath("$.message").value(ErrorCode.EMAIL_NOT_VERIFIED.getMessage()));
            }
        }

        @Nested
        @DisplayName("로그인 실패 - 유효성 검증 오류")
        class LoginValidationFailureTest {

            @Test
            @DisplayName("학번 빈 값 - 400 Bad Request [LOG-013]")
            void login_withEmptyStudentId_returns400() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.loginRequestWithEmptyStudentId();

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
            }

            @Test
            @DisplayName("비밀번호 빈 값 - 400 Bad Request [LOG-014]")
            void login_withEmptyPassword_returns400() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.loginRequestWithEmptyPassword();

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
            }

            @Test
            @DisplayName("학번 공백 값 - 400 Bad Request")
            void login_withBlankStudentId_returns400() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.loginRequestWithBlankStudentId();

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
            }

            @Test
            @DisplayName("비밀번호 공백 값 - 400 Bad Request")
            void login_withBlankPassword_returns400() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.loginRequestWithBlankPassword();

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
            }
        }

        @Nested
        @DisplayName("로그인 실패 - 계정 상태 오류")
        class LoginAccountStatusFailureTest {

            @Test
            @DisplayName("정지된 계정 - 403 Forbidden [LOG-020]")
            void login_withSuspendedAccount_returns403() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.validLoginRequest();

                given(passwordAuthService.login(any(PasswordLoginRequest.class), anyString(), any()))
                        .willThrow(new AccountSuspendedException());

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(ErrorCode.ACCOUNT_SUSPENDED.getCode()))
                        .andExpect(jsonPath("$.message").value(ErrorCode.ACCOUNT_SUSPENDED.getMessage()));
            }

            @Test
            @DisplayName("탈퇴한 계정 - 403 Forbidden [LOG-021]")
            void login_withWithdrawnAccount_returns403() throws Exception {
                // given
                PasswordLoginRequest request = PasswordAuthTestFixture.validLoginRequest();

                given(passwordAuthService.login(any(PasswordLoginRequest.class), anyString(), any()))
                        .willThrow(new AccountWithdrawnException());

                // when & then
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(ErrorCode.ACCOUNT_WITHDRAWN.getCode()))
                        .andExpect(jsonPath("$.message").value(ErrorCode.ACCOUNT_WITHDRAWN.getMessage()));
            }
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {

        @Nested
        @DisplayName("로그아웃 성공")
        class LogoutSuccessTest {

            @Test
            @DisplayName("로그아웃 성공 - 200 OK [LOG-030]")
            void logout_withValidToken_returns200() throws Exception {
                // given
                willDoNothing().given(passwordAuthService).logout(anyString());

                // when & then
                mockMvc.perform(post(LOGOUT_URL)
                                .cookie(new Cookie("refreshToken", PasswordAuthTestFixture.VALID_REFRESH_TOKEN)))
                        .andDo(print())
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("로그아웃 실패")
        class LogoutFailureTest {

            @Test
            @DisplayName("유효하지 않은 토큰 - 401 Unauthorized [LOG-031]")
            void logout_withInvalidToken_returns401() throws Exception {
                // given
                willThrow(new RefreshTokenInvalidException())
                        .given(passwordAuthService).logout(anyString());

                // when & then
                mockMvc.perform(post(LOGOUT_URL)
                                .cookie(new Cookie("refreshToken", PasswordAuthTestFixture.INVALID_REFRESH_TOKEN)))
                        .andDo(print())
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_INVALID.getCode()))
                        .andExpect(jsonPath("$.message").value(ErrorCode.REFRESH_TOKEN_INVALID.getMessage()));
            }

            @Test
            @DisplayName("쿠키 없음 - 401 Unauthorized [LOG-032]")
            void logout_withNoCookie_returns401() throws Exception {
                // given - 쿠키 없이 요청: getRefreshTokenFromCookies가 빈 Optional 반환
                given(cookieUtil.getRefreshTokenFromCookies(any()))
                        .willReturn(Optional.empty());

                // when & then
                mockMvc.perform(post(LOGOUT_URL))
                        .andDo(print())
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_INVALID.getCode()));
            }
        }
    }
}
