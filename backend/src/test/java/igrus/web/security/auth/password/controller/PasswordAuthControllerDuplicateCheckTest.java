package igrus.web.security.auth.password.controller;

import igrus.web.common.exception.ErrorCode;
import igrus.web.common.exception.GlobalExceptionHandler;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.security.auth.common.service.account.CheckReRegistrationEligibilityService;
import igrus.web.security.auth.common.service.account.CheckRecoveryEligibilityService;
import igrus.web.security.auth.common.service.account.RecoverAccountService;
import igrus.web.security.auth.common.service.AccountStatusService;
import igrus.web.security.auth.common.util.CookieUtil;
import igrus.web.security.auth.password.dto.response.DuplicateCheckResponse;
import igrus.web.security.auth.password.service.reset.RequestPasswordResetService;
import igrus.web.security.auth.password.service.reset.ResetPasswordService;
import igrus.web.security.auth.password.service.reset.ValidateResetTokenService;
import igrus.web.security.auth.password.service.signup.CheckDuplicateService;
import igrus.web.security.auth.password.service.signup.ResendVerificationService;
import igrus.web.security.auth.password.service.signup.SignupService;
import igrus.web.security.auth.password.service.signup.VerifyEmailService;
import igrus.web.security.auth.password.service.auth.LoginService;
import igrus.web.security.auth.password.service.auth.LogoutService;
import igrus.web.security.auth.password.service.auth.RefreshTokenService;
import igrus.web.security.config.ApiSecurityConfig;
import igrus.web.security.config.SecurityConfigUtil;
import igrus.web.security.jwt.JwtAuthenticationEntryPoint;
import igrus.web.security.jwt.JwtAuthenticationFilter;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.exception.InvalidEmailException;
import igrus.web.user.exception.InvalidStudentIdException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordAuthController.class)
@Import({GlobalExceptionHandler.class, ApiSecurityConfig.class, SecurityConfigUtil.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
@DisplayName("PasswordAuthController 중복 체크 테스트")
class PasswordAuthControllerDuplicateCheckTest {

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
    private CheckDuplicateService checkDuplicateService;

    @MockitoBean
    private VerifyEmailService verifyEmailService;

    @MockitoBean
    private ResendVerificationService resendVerificationService;

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

    private static final String CHECK_STUDENT_ID_URL = "/api/v1/auth/password/check-student-id";
    private static final String CHECK_EMAIL_URL = "/api/v1/auth/password/check-email";

    @Nested
    @DisplayName("학번 중복 체크 API")
    class CheckStudentIdTest {

        @Test
        @DisplayName("사용 가능한 학번 - 200 반환")
        void checkStudentId_WhenAvailable_Returns200() throws Exception {
            // given
            given(checkDuplicateService.checkStudentId("12345678"))
                    .willReturn(DuplicateCheckResponse.studentIdAvailable());

            // when & then
            mockMvc.perform(get(CHECK_STUDENT_ID_URL)
                            .param("studentId", "12345678"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(true))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("학번 형식 오류 - 400 반환")
        void checkStudentId_WhenInvalidFormat_Returns400() throws Exception {
            // given
            given(checkDuplicateService.checkStudentId("1234"))
                    .willThrow(new InvalidStudentIdException("1234"));

            // when & then
            mockMvc.perform(get(CHECK_STUDENT_ID_URL)
                            .param("studentId", "1234"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_STUDENT_ID.getCode()));
        }

        @Test
        @DisplayName("중복 학번 - 409 반환")
        void checkStudentId_WhenDuplicate_Returns409() throws Exception {
            // given
            given(checkDuplicateService.checkStudentId("12345678"))
                    .willThrow(new DuplicateStudentIdException());

            // when & then
            mockMvc.perform(get(CHECK_STUDENT_ID_URL)
                            .param("studentId", "12345678"))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_STUDENT_ID.getCode()));
        }

        @Test
        @DisplayName("파라미터 누락 - 400 반환")
        void checkStudentId_WhenMissingParam_Returns400() throws Exception {
            mockMvc.perform(get(CHECK_STUDENT_ID_URL))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("이메일 중복 체크 API")
    class CheckEmailTest {

        @Test
        @DisplayName("사용 가능한 이메일 - 200 반환")
        void checkEmail_WhenAvailable_Returns200() throws Exception {
            // given
            given(checkDuplicateService.checkEmail("user@example.com"))
                    .willReturn(DuplicateCheckResponse.emailAvailable());

            // when & then
            mockMvc.perform(get(CHECK_EMAIL_URL)
                            .param("email", "user@example.com"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(true))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("이메일 형식 오류 - 400 반환")
        void checkEmail_WhenInvalidFormat_Returns400() throws Exception {
            // given
            given(checkDuplicateService.checkEmail("invalid-email"))
                    .willThrow(new InvalidEmailException("invalid-email"));

            // when & then
            mockMvc.perform(get(CHECK_EMAIL_URL)
                            .param("email", "invalid-email"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_EMAIL_FORMAT.getCode()));
        }

        @Test
        @DisplayName("중복 이메일 - 409 반환")
        void checkEmail_WhenDuplicate_Returns409() throws Exception {
            // given
            given(checkDuplicateService.checkEmail("user@example.com"))
                    .willThrow(new DuplicateEmailException());

            // when & then
            mockMvc.perform(get(CHECK_EMAIL_URL)
                            .param("email", "user@example.com"))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_EMAIL.getCode()));
        }

        @Test
        @DisplayName("파라미터 누락 - 400 반환")
        void checkEmail_WhenMissingParam_Returns400() throws Exception {
            mockMvc.perform(get(CHECK_EMAIL_URL))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}
