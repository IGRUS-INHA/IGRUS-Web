package igrus.web.security.auth.password.controller;

import tools.jackson.databind.json.JsonMapper;
import igrus.web.common.exception.ErrorCode;
import igrus.web.common.exception.GlobalExceptionHandler;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.common.exception.verification.VerificationAttemptsExceededException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeExpiredException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeInvalidException;
import igrus.web.security.auth.common.exception.verification.VerificationResendRateLimitedException;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.auth.password.service.PasswordSignupService;
import igrus.web.security.config.ApiSecurityConfig;
import igrus.web.security.config.SecurityConfigUtil;
import igrus.web.security.jwt.JwtAuthenticationFilter;
import igrus.web.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordAuthController.class)
@Import({GlobalExceptionHandler.class, ApiSecurityConfig.class, SecurityConfigUtil.class, JwtAuthenticationFilter.class})
@DisplayName("PasswordAuthController 이메일 인증 테스트")
class PasswordAuthControllerVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @MockitoBean
    private PasswordAuthService passwordAuthService;

    @MockitoBean
    private PasswordSignupService passwordSignupService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final String VALID_EMAIL = "test@inha.edu";
    private static final String VALID_CODE = "123456";
    private static final String VERIFY_EMAIL_URL = "/api/v1/auth/password/verify-email";
    private static final String RESEND_VERIFICATION_URL = "/api/v1/auth/password/resend-verification";

    @Nested
    @DisplayName("이메일 인증 테스트")
    class VerifyEmailTest {

        @Nested
        @DisplayName("인증 성공")
        class VerifySuccessTest {

            @Test
            @DisplayName("[REG-041] 올바른 인증 코드 입력 시 200 OK 반환")
            void verifyEmail_WithValidCode_Returns200() throws Exception {
                // given
                EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, VALID_CODE);
                PasswordSignupResponse response = PasswordSignupResponse.verified(VALID_EMAIL);

                given(passwordSignupService.verifyEmail(any(EmailVerificationRequest.class)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(post(VERIFY_EMAIL_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))
                                )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                        .andExpect(jsonPath("$.requiresVerification").value(false));
            }
        }

        @Nested
        @DisplayName("인증 실패")
        class VerifyFailureTest {

            @Test
                        @DisplayName("[REG-042] 만료된 인증 코드 입력 시 400 Bad Request 반환")
            void verifyEmail_WithExpiredCode_Returns400() throws Exception {
                // given
                EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, VALID_CODE);

                willThrow(new VerificationCodeExpiredException())
                        .given(passwordSignupService).verifyEmail(any(EmailVerificationRequest.class));

                // when & then
                mockMvc.perform(post(VERIFY_EMAIL_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))
                                )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.VERIFICATION_CODE_EXPIRED.getCode()));
            }

            @Test
                        @DisplayName("[REG-043] 인증 시도 횟수 초과 시 429 Too Many Requests 반환")
            void verifyEmail_WithExceededAttempts_Returns429() throws Exception {
                // given
                EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, VALID_CODE);

                willThrow(new VerificationAttemptsExceededException())
                        .given(passwordSignupService).verifyEmail(any(EmailVerificationRequest.class));

                // when & then
                mockMvc.perform(post(VERIFY_EMAIL_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))
                                )
                        .andExpect(status().isTooManyRequests())
                        .andExpect(jsonPath("$.code").value(ErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED.getCode()));
            }

            @Test
                        @DisplayName("잘못된 인증 코드 입력 시 400 Bad Request 반환")
            void verifyEmail_WithInvalidCode_Returns400() throws Exception {
                // given
                EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, "000000");

                willThrow(new VerificationCodeInvalidException())
                        .given(passwordSignupService).verifyEmail(any(EmailVerificationRequest.class));

                // when & then
                mockMvc.perform(post(VERIFY_EMAIL_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))
                                )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.VERIFICATION_CODE_INVALID.getCode()));
            }

            @Test
                        @DisplayName("이메일 형식 오류 시 400 Bad Request 반환")
            void verifyEmail_WithInvalidEmailFormat_Returns400() throws Exception {
                // given
                String invalidRequest = """
                        {
                            "email": "invalid-email",
                            "code": "123456"
                        }
                        """;

                // when & then
                mockMvc.perform(post(VERIFY_EMAIL_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                                )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
            }

            @Test
                        @DisplayName("인증 코드 빈 값 시 400 Bad Request 반환")
            void verifyEmail_WithEmptyCode_Returns400() throws Exception {
                // given
                String invalidRequest = """
                        {
                            "email": "test@inha.edu",
                            "code": ""
                        }
                        """;

                // when & then
                mockMvc.perform(post(VERIFY_EMAIL_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                                )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
            }

            @Test
                        @DisplayName("인증 코드 6자리 미만 시 400 Bad Request 반환")
            void verifyEmail_WithCodeLessThan6Digits_Returns400() throws Exception {
                // given
                String invalidRequest = """
                        {
                            "email": "test@inha.edu",
                            "code": "12345"
                        }
                        """;

                // when & then
                mockMvc.perform(post(VERIFY_EMAIL_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                                )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
            }

            @Test
                        @DisplayName("인증 코드 6자리 초과 시 400 Bad Request 반환")
            void verifyEmail_WithCodeMoreThan6Digits_Returns400() throws Exception {
                // given
                String invalidRequest = """
                        {
                            "email": "test@inha.edu",
                            "code": "1234567"
                        }
                        """;

                // when & then
                mockMvc.perform(post(VERIFY_EMAIL_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                                )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
            }

            @Test
                        @DisplayName("이메일 빈 값 시 400 Bad Request 반환")
            void verifyEmail_WithEmptyEmail_Returns400() throws Exception {
                // given
                String invalidRequest = """
                        {
                            "email": "",
                            "code": "123456"
                        }
                        """;

                // when & then
                mockMvc.perform(post(VERIFY_EMAIL_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                                )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("인증 코드 재발송 테스트")
    class ResendVerificationTest {

        @Test
                @DisplayName("[REG-045] 재발송 성공 시 200 OK 반환")
        void resendVerification_Success_Returns200() throws Exception {
            // given
            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);
            PasswordSignupResponse response = PasswordSignupResponse.pendingVerification(VALID_EMAIL);

            given(passwordSignupService.resendVerification(any(ResendVerificationRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post(RESEND_VERIFICATION_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                    .andExpect(jsonPath("$.requiresVerification").value(true));
        }

        @Test
                @DisplayName("[REG-044] 재발송 제한 (rate limit) 시 429 Too Many Requests 반환")
        void resendVerification_RateLimited_Returns429() throws Exception {
            // given
            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            willThrow(new VerificationResendRateLimitedException())
                    .given(passwordSignupService).resendVerification(any(ResendVerificationRequest.class));

            // when & then
            mockMvc.perform(post(RESEND_VERIFICATION_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                            )
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value(ErrorCode.VERIFICATION_RESEND_RATE_LIMITED.getCode()));
        }

        @Test
                @DisplayName("이메일 형식 오류 시 400 Bad Request 반환")
        void resendVerification_WithInvalidEmailFormat_Returns400() throws Exception {
            // given
            String invalidRequest = """
                    {
                        "email": "invalid-email"
                    }
                    """;

            // when & then
            mockMvc.perform(post(RESEND_VERIFICATION_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest)
                            )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
                @DisplayName("이메일 빈 값 시 400 Bad Request 반환")
        void resendVerification_WithEmptyEmail_Returns400() throws Exception {
            // given
            String invalidRequest = """
                    {
                        "email": ""
                    }
                    """;

            // when & then
            mockMvc.perform(post(RESEND_VERIFICATION_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest)
                            )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }
    }
}
