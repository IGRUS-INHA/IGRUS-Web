package igrus.web.security.auth.password.controller;

import igrus.web.common.exception.ErrorCode;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.common.exception.verification.VerificationAttemptsExceededException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeExpiredException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeInvalidException;
import igrus.web.security.auth.common.exception.verification.VerificationResendRateLimitedException;
import igrus.web.security.auth.password.dto.response.PreSignupVerificationResponse;
import igrus.web.security.auth.password.dto.response.VerificationResendResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PasswordAuthController 이메일 인증 테스트")
class PasswordAuthControllerVerificationTest extends PasswordAuthControllerTestBase {

    private static final String VALID_EMAIL = "test@inha.edu";
    private static final String VALID_CODE = "123456";
    private static final String VERIFY_CODE_URL = "/api/v1/auth/password/pre-signup/verify-code";
    private static final String SEND_CODE_URL = "/api/v1/auth/password/pre-signup/send-code";

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
                PreSignupVerificationResponse response = PreSignupVerificationResponse.success(VALID_EMAIL, "test-token");

                given(preSignupVerifyCodeService.verifyCode(any(EmailVerificationRequest.class)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(post(VERIFY_CODE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                        .andExpect(jsonPath("$.verified").value(true))
                        .andExpect(jsonPath("$.verificationToken").exists());
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
                        .given(preSignupVerifyCodeService).verifyCode(any(EmailVerificationRequest.class));

                // when & then
                mockMvc.perform(post(VERIFY_CODE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
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
                        .given(preSignupVerifyCodeService).verifyCode(any(EmailVerificationRequest.class));

                // when & then
                mockMvc.perform(post(VERIFY_CODE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
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
                        .given(preSignupVerifyCodeService).verifyCode(any(EmailVerificationRequest.class));

                // when & then
                mockMvc.perform(post(VERIFY_CODE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
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
                mockMvc.perform(post(VERIFY_CODE_URL)
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
                mockMvc.perform(post(VERIFY_CODE_URL)
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
                mockMvc.perform(post(VERIFY_CODE_URL)
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
                mockMvc.perform(post(VERIFY_CODE_URL)
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
                mockMvc.perform(post(VERIFY_CODE_URL)
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
            VerificationResendResponse response = VerificationResendResponse.sent(VALID_EMAIL);

            given(preSignupSendCodeService.sendCode(any(ResendVerificationRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post(SEND_CODE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                    .andExpect(jsonPath("$.message").value("인증 코드가 발송되었습니다."));
        }

        @Test
                @DisplayName("[REG-044] 재발송 제한 (rate limit) 시 429 Too Many Requests 반환")
        void resendVerification_RateLimited_Returns429() throws Exception {
            // given
            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            willThrow(new VerificationResendRateLimitedException())
                    .given(preSignupSendCodeService).sendCode(any(ResendVerificationRequest.class));

            // when & then
            mockMvc.perform(post(SEND_CODE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
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
            mockMvc.perform(post(SEND_CODE_URL)
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
            mockMvc.perform(post(SEND_CODE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest)
                            )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }
    }
}
