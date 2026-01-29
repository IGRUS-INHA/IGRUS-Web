package igrus.web.security.auth.password.controller;

import igrus.web.common.exception.ErrorCode;
import igrus.web.common.exception.GlobalExceptionHandler;
import igrus.web.security.auth.common.dto.internal.RecoveryResult;
import igrus.web.security.auth.common.dto.request.AccountRecoveryRequest;
import igrus.web.security.auth.common.dto.response.RecoveryEligibilityResponse;
import igrus.web.security.auth.common.exception.account.AccountNotRecoverableException;
import igrus.web.security.auth.common.service.AccountRecoveryService;
import igrus.web.security.auth.common.service.AccountStatusService;
import igrus.web.security.auth.common.util.CookieUtil;
import igrus.web.security.auth.password.controller.fixture.PasswordAuthTestFixture;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.auth.password.service.PasswordResetService;
import igrus.web.security.auth.password.service.PasswordSignupService;
import igrus.web.security.config.ApiSecurityConfig;
import igrus.web.security.config.SecurityConfigUtil;
import igrus.web.security.jwt.JwtAuthenticationFilter;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ApiSecurityConfig.class, SecurityConfigUtil.class, JwtAuthenticationFilter.class})
@DisplayName("PasswordAuthController 계정 복구 테스트")
class PasswordAuthControllerAccountRecoveryTest {

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
    private AccountRecoveryService accountRecoveryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private SecurityConfigUtil securityConfigUtil;

    @MockitoBean
    private AccountStatusService accountStatusService;

    @MockitoBean
    private CookieUtil cookieUtil;

    private static final String RECOVERY_CHECK_URL = "/api/v1/auth/password/account/recovery-check";
    private static final String RECOVERY_URL = "/api/v1/auth/password/account/recover";

    @BeforeEach
    void setUp() {
        // Mock for account recovery - createRefreshTokenCookie
        given(cookieUtil.createRefreshTokenCookie(anyString(), any(Duration.class)))
                .willReturn(ResponseCookie.from("refreshToken", "test-token")
                        .httpOnly(true)
                        .path("/api/v1/auth")
                        .build());
    }

    @Nested
    @DisplayName("복구 가능 여부 조회 테스트")
    class RecoveryCheckTest {

        @Nested
        @DisplayName("복구 가능 여부 조회 성공")
        class RecoveryCheckSuccessTest {

            @Test
            @DisplayName("복구 가능한 계정 조회 - 200 OK [REC-001]")
            void checkRecoveryEligibility_withRecoverableAccount_returns200() throws Exception {
                // given
                RecoveryEligibilityResponse response = PasswordAuthTestFixture.recoverableResponse();

                given(accountRecoveryService.checkRecoveryEligibility(PasswordAuthTestFixture.VALID_STUDENT_ID))
                        .willReturn(response);

                // when & then
                mockMvc.perform(get(RECOVERY_CHECK_URL)
                                .param("studentId", PasswordAuthTestFixture.VALID_STUDENT_ID))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.recoverable").value(true))
                        .andExpect(jsonPath("$.recoveryDeadline").exists())
                        .andExpect(jsonPath("$.message").value("탈퇴한 계정입니다. 복구하시겠습니까?"));
            }

            @Test
            @DisplayName("복구 불가능한 계정 조회 (5일 초과) - 200 OK [REC-002]")
            void checkRecoveryEligibility_withExpiredRecoveryPeriod_returns200WithNotRecoverable() throws Exception {
                // given
                RecoveryEligibilityResponse response = PasswordAuthTestFixture.notRecoverableResponse();

                given(accountRecoveryService.checkRecoveryEligibility(PasswordAuthTestFixture.VALID_STUDENT_ID))
                        .willReturn(response);

                // when & then
                mockMvc.perform(get(RECOVERY_CHECK_URL)
                                .param("studentId", PasswordAuthTestFixture.VALID_STUDENT_ID))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.recoverable").value(false))
                        .andExpect(jsonPath("$.recoveryDeadline").doesNotExist())
                        .andExpect(jsonPath("$.message").value("복구 기간이 만료된 계정입니다"));
            }

            @Test
            @DisplayName("탈퇴 상태가 아닌 계정 조회 - 200 OK [REC-003]")
            void checkRecoveryEligibility_withNonWithdrawnAccount_returns200WithNotWithdrawn() throws Exception {
                // given
                RecoveryEligibilityResponse response = PasswordAuthTestFixture.notWithdrawnResponse();

                given(accountRecoveryService.checkRecoveryEligibility(PasswordAuthTestFixture.VALID_STUDENT_ID))
                        .willReturn(response);

                // when & then
                mockMvc.perform(get(RECOVERY_CHECK_URL)
                                .param("studentId", PasswordAuthTestFixture.VALID_STUDENT_ID))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.recoverable").value(false))
                        .andExpect(jsonPath("$.message").value("탈퇴 상태가 아닌 계정입니다"));
            }
        }

        @Nested
        @DisplayName("복구 가능 여부 조회 실패 - 유효성 검증")
        class RecoveryCheckValidationFailureTest {

            @Test
            @DisplayName("학번 형식 오류 (8자리가 아닌 경우) - 400 Bad Request [REC-004]")
            void checkRecoveryEligibility_withInvalidStudentIdFormat_returns400() throws Exception {
                // when & then
                mockMvc.perform(get(RECOVERY_CHECK_URL)
                                .param("studentId", PasswordAuthTestFixture.INVALID_FORMAT_STUDENT_ID))
                        .andDo(print())
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("학번 누락 - 400 Bad Request [REC-005]")
            void checkRecoveryEligibility_withMissingStudentId_returns400() throws Exception {
                // when & then
                mockMvc.perform(get(RECOVERY_CHECK_URL))
                        .andDo(print())
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("계정 복구 테스트")
    class RecoverAccountTest {

        @Nested
        @DisplayName("계정 복구 성공")
        class RecoverAccountSuccessTest {

            @Test
            @DisplayName("유효한 요청으로 계정 복구 성공 - 200 OK [REC-010]")
            void recoverAccount_withValidRequest_returns200() throws Exception {
                // given
                AccountRecoveryRequest request = PasswordAuthTestFixture.validRecoveryRequest();
                RecoveryResult result = PasswordAuthTestFixture.recoverySuccessResult();

                given(accountRecoveryService.recoverAccount(
                        PasswordAuthTestFixture.VALID_STUDENT_ID,
                        PasswordAuthTestFixture.VALID_PASSWORD))
                        .willReturn(result);

                // when & then - refreshToken은 쿠키로 설정됨
                mockMvc.perform(post(RECOVERY_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").value(PasswordAuthTestFixture.VALID_ACCESS_TOKEN))
                        .andExpect(jsonPath("$.userId").value(PasswordAuthTestFixture.VALID_USER_ID))
                        .andExpect(jsonPath("$.studentId").value(PasswordAuthTestFixture.VALID_STUDENT_ID))
                        .andExpect(jsonPath("$.name").value(PasswordAuthTestFixture.VALID_NAME))
                        .andExpect(jsonPath("$.role").value(UserRole.MEMBER.name()))
                        .andExpect(jsonPath("$.expiresIn").value(PasswordAuthTestFixture.VALID_EXPIRES_IN))
                        .andExpect(jsonPath("$.message").value("계정이 성공적으로 복구되었습니다"));
            }
        }

        @Nested
        @DisplayName("계정 복구 실패 - 인증 오류")
        class RecoverAccountAuthenticationFailureTest {

            @Test
            @DisplayName("잘못된 비밀번호 - 401 Unauthorized [REC-011]")
            void recoverAccount_withInvalidPassword_returns401() throws Exception {
                // given
                AccountRecoveryRequest request = PasswordAuthTestFixture.recoveryRequestWithInvalidPassword();

                given(accountRecoveryService.recoverAccount(
                        PasswordAuthTestFixture.VALID_STUDENT_ID,
                        PasswordAuthTestFixture.INVALID_PASSWORD))
                        .willThrow(new InvalidCredentialsException());

                // when & then
                mockMvc.perform(post(RECOVERY_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()))
                        .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_CREDENTIALS.getMessage()));
            }

            @Test
            @DisplayName("존재하지 않는 계정 - 401 Unauthorized [REC-012]")
            void recoverAccount_withNonExistentAccount_returns401() throws Exception {
                // given
                AccountRecoveryRequest request = new AccountRecoveryRequest(
                        PasswordAuthTestFixture.INVALID_STUDENT_ID,
                        PasswordAuthTestFixture.VALID_PASSWORD
                );

                given(accountRecoveryService.recoverAccount(
                        PasswordAuthTestFixture.INVALID_STUDENT_ID,
                        PasswordAuthTestFixture.VALID_PASSWORD))
                        .willThrow(new InvalidCredentialsException());

                // when & then
                mockMvc.perform(post(RECOVERY_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()));
            }
        }

        @Nested
        @DisplayName("계정 복구 실패 - 복구 불가")
        class RecoverAccountNotRecoverableTest {

            @Test
            @DisplayName("복구 기간 만료 - 400 Bad Request [REC-013]")
            void recoverAccount_withExpiredRecoveryPeriod_returns400() throws Exception {
                // given
                AccountRecoveryRequest request = PasswordAuthTestFixture.validRecoveryRequest();

                given(accountRecoveryService.recoverAccount(
                        PasswordAuthTestFixture.VALID_STUDENT_ID,
                        PasswordAuthTestFixture.VALID_PASSWORD))
                        .willThrow(new AccountNotRecoverableException());

                // when & then
                mockMvc.perform(post(RECOVERY_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.ACCOUNT_NOT_RECOVERABLE.getCode()))
                        .andExpect(jsonPath("$.message").value(ErrorCode.ACCOUNT_NOT_RECOVERABLE.getMessage()));
            }
        }

        @Nested
        @DisplayName("계정 복구 실패 - 유효성 검증 오류")
        class RecoverAccountValidationFailureTest {

            @Test
            @DisplayName("학번 빈 값 - 400 Bad Request [REC-014]")
            void recoverAccount_withEmptyStudentId_returns400() throws Exception {
                // given
                AccountRecoveryRequest request = PasswordAuthTestFixture.recoveryRequestWithEmptyStudentId();

                // when & then
                mockMvc.perform(post(RECOVERY_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
            }

            @Test
            @DisplayName("비밀번호 빈 값 - 400 Bad Request [REC-015]")
            void recoverAccount_withEmptyPassword_returns400() throws Exception {
                // given
                AccountRecoveryRequest request = PasswordAuthTestFixture.recoveryRequestWithEmptyPassword();

                // when & then
                mockMvc.perform(post(RECOVERY_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
            }

            @Test
            @DisplayName("학번 형식 오류 - 400 Bad Request [REC-016]")
            void recoverAccount_withInvalidStudentIdFormat_returns400() throws Exception {
                // given
                AccountRecoveryRequest request = PasswordAuthTestFixture.recoveryRequestWithInvalidStudentIdFormat();

                // when & then
                mockMvc.perform(post(RECOVERY_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
            }
        }
    }
}
