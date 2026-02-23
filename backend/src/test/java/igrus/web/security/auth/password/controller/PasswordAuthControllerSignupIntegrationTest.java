package igrus.web.security.auth.password.controller;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.Interest;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 회원가입 HTTP 컨트롤러 통합 테스트 (T024)
 *
 * <p>MockMvc를 사용한 HTTP 레벨 통합 테스트입니다.</p>
 *
 * <p>테스트 범위:</p>
 * <ul>
 *     <li>REG-001 ~ REG-003: 회원가입 성공 케이스 (사전 이메일 인증 → 가입)</li>
 *     <li>REG-010 ~ REG-017: 회원가입 실패 케이스 (유효성 검증)</li>
 *     <li>PRE-001 ~ PRE-005: 사전 이메일 인증 케이스</li>
 * </ul>
 */
@DisplayName("회원가입 HTTP 컨트롤러 통합 테스트")
class PasswordAuthControllerSignupIntegrationTest extends ControllerIntegrationTestBase {

    private String verificationToken;

    @BeforeEach
    void setUp() {
        setUpControllerTest();
    }

    private PasswordSignupRequest createValidSignupRequest() {
        return new PasswordSignupRequest(
                TEST_STUDENT_ID,
                TEST_NAME,
                TEST_EMAIL,
                TEST_PASSWORD,
                TEST_PHONE,
                TEST_DEPARTMENT,
                TEST_MOTIVATION,
                List.of(),
                List.of(Interest.WEB_FRONTEND),
                null,
                JoinRoute.EVERYTIME,
                null,
                Gender.MALE,
                1,
                EnrollmentStatus.ENROLLED,
                true,
                verificationToken
        );
    }

    private PasswordSignupRequest createSignupRequest(String studentId, String name, String email,
                                                       String password, String phone, String department,
                                                       String motivation, Boolean privacyConsent) {
        return new PasswordSignupRequest(
                studentId, name, email, password, phone, department, motivation, List.of(),
                List.of(Interest.WEB_FRONTEND), null, JoinRoute.EVERYTIME, null,
                Gender.MALE, 1, EnrollmentStatus.ENROLLED, privacyConsent,
                verificationToken
        );
    }

    /**
     * 사전 인증된 이메일 레코드를 생성합니다.
     */
    private void createVerifiedEmailRecord(String email) {
        EmailVerification verification = EmailVerification.create(email, "123456", 600000L);
        this.verificationToken = verification.verify();
        emailVerificationRepository.save(verification);
    }

    // ===== 회원가입 성공 테스트 =====

    @Nested
    @DisplayName("회원가입 성공 테스트")
    class SignupSuccessTest {

        @Test
        @DisplayName("[REG-001] 사전 인증 후 회원가입 요청 성공 - 201 응답")
        void signup_withPreVerifiedEmail_returns201() throws Exception {
            // given - 이메일 사전 인증
            createVerifiedEmailRecord(TEST_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            performPost("/signup", request)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.requiresVerification").doesNotExist());

            // DB 상태 확인
            User savedUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(savedUser.getStudentId()).isEqualTo(TEST_STUDENT_ID);
        }

        @Test
        @DisplayName("[REG-002] 이메일 미인증 상태에서 가입 시도 - 400 응답")
        void signup_withoutPreVerifiedEmail_returns400() throws Exception {
            // given - 이메일 사전 인증 없이 가입 시도
            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            performPost("/signup", request)
                    .andExpect(status().isBadRequest());

            // 사용자가 생성되지 않았는지 확인
            assertThat(userRepository.findByEmail(TEST_EMAIL)).isEmpty();
        }
    }

    // ===== 회원가입 실패 테스트 (유효성 검증) =====

    @Nested
    @DisplayName("회원가입 유효성 검증 실패 테스트")
    class SignupValidationFailureTest {

        @Test
        @DisplayName("[REG-010] 중복 학번 가입 시도 - 409 Conflict 응답")
        void signup_withDuplicateStudentId_returns409() throws Exception {
            // given - 기존 사용자 생성
            createAndSaveDefaultUserWithCredential();
            createVerifiedEmailRecord("other@inha.edu");

            // 동일 학번으로 가입 시도
            PasswordSignupRequest request = createSignupRequest(
                    TEST_STUDENT_ID, "다른이름", "other@inha.edu",
                    TEST_PASSWORD, "010-9999-8888", TEST_DEPARTMENT,
                    TEST_MOTIVATION, true
            );

            // when & then
            performPost("/signup", request)
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("[REG-011] 중복 이메일 가입 시도 - 409 Conflict 응답")
        void signup_withDuplicateEmail_returns409() throws Exception {
            // given - 기존 사용자 생성
            createAndSaveDefaultUserWithCredential();
            createVerifiedEmailRecord(TEST_EMAIL);

            // 동일 이메일로 가입 시도
            PasswordSignupRequest request = createSignupRequest(
                    "99999999", "다른이름", TEST_EMAIL,
                    TEST_PASSWORD, "010-9999-8888", TEST_DEPARTMENT,
                    TEST_MOTIVATION, true
            );

            // when & then
            performPost("/signup", request)
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("[REG-012] 잘못된 이메일 형식 - 400 Bad Request 응답")
        void signup_withInvalidEmailFormat_returns400() throws Exception {
            // given
            PasswordSignupRequest request = createSignupRequest(
                    TEST_STUDENT_ID, TEST_NAME, "invalid-email",
                    TEST_PASSWORD, TEST_PHONE, TEST_DEPARTMENT,
                    TEST_MOTIVATION, true
            );

            // when & then
            performPost("/signup", request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[REG-013] 비밀번호 정책 미충족 (영문 없음) - 400 Bad Request 응답")
        void signup_withPasswordMissingLetter_returns400() throws Exception {
            // given
            PasswordSignupRequest request = createSignupRequest(
                    TEST_STUDENT_ID, TEST_NAME, TEST_EMAIL,
                    "12345678", // 영문 없음
                    TEST_PHONE, TEST_DEPARTMENT,
                    TEST_MOTIVATION, true
            );

            // when & then
            performPost("/signup", request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[REG-014] 비밀번호 정책 미충족 (숫자 없음) - 400 Bad Request 응답")
        void signup_withPasswordMissingDigit_returns400() throws Exception {
            // given
            PasswordSignupRequest request = createSignupRequest(
                    TEST_STUDENT_ID, TEST_NAME, TEST_EMAIL,
                    "password", // 숫자 없음
                    TEST_PHONE, TEST_DEPARTMENT,
                    TEST_MOTIVATION, true
            );

            // when & then
            performPost("/signup", request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[REG-015] 개인정보 동의 미체크 - 400 Bad Request 응답")
        void signup_withoutPrivacyConsent_returns400() throws Exception {
            // given
            PasswordSignupRequest request = createSignupRequest(
                    TEST_STUDENT_ID, TEST_NAME, TEST_EMAIL,
                    TEST_PASSWORD, TEST_PHONE, TEST_DEPARTMENT,
                    TEST_MOTIVATION, false
            );

            // when & then
            performPost("/signup", request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[REG-016] 학번 형식 오류 (8자리 아님) - 400 Bad Request 응답")
        void signup_withInvalidStudentIdFormat_returns400() throws Exception {
            // given
            PasswordSignupRequest request = createSignupRequest(
                    "1234", // 4자리
                    TEST_NAME, TEST_EMAIL,
                    TEST_PASSWORD, TEST_PHONE, TEST_DEPARTMENT,
                    TEST_MOTIVATION, true
            );

            // when & then
            performPost("/signup", request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[REG-017] 필수 필드 누락 (이름) - 400 Bad Request 응답")
        void signup_withMissingName_returns400() throws Exception {
            // given
            PasswordSignupRequest request = createSignupRequest(
                    TEST_STUDENT_ID, "", TEST_EMAIL,
                    TEST_PASSWORD, TEST_PHONE, TEST_DEPARTMENT,
                    TEST_MOTIVATION, true
            );

            // when & then
            performPost("/signup", request)
                    .andExpect(status().isBadRequest());
        }
    }

    // ===== 사전 이메일 인증 테스트 =====

    @Nested
    @DisplayName("사전 이메일 인증 테스트")
    class PreSignupVerificationTest {

        @Test
        @DisplayName("[PRE-001] 인증 코드 발송 성공 - 200 OK")
        void sendCode_withValidEmail_returns200() throws Exception {
            // given
            ResendVerificationRequest request = new ResendVerificationRequest(TEST_EMAIL);

            // when & then
            performPost("/pre-signup/send-code", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.message").exists());

            verify(authEmailService).sendVerificationEmail(eq(TEST_EMAIL), anyString());
        }

        @Test
        @DisplayName("[PRE-002] 인증 코드 확인 성공 - 200 OK")
        void verifyCode_withValidCode_returns200() throws Exception {
            // given - 코드 발송
            ResendVerificationRequest sendRequest = new ResendVerificationRequest(TEST_EMAIL);
            performPost("/pre-signup/send-code", sendRequest)
                    .andExpect(status().isOk());

            // 코드 조회
            String code = getVerificationCode(TEST_EMAIL);
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(TEST_EMAIL, code);

            // when & then
            performPost("/pre-signup/verify-code", verifyRequest)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verified").value(true))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL));
        }

        @Test
        @DisplayName("[PRE-003] 잘못된 인증 코드 - 400 Bad Request")
        void verifyCode_withInvalidCode_returns400() throws Exception {
            // given - 코드 발송
            ResendVerificationRequest sendRequest = new ResendVerificationRequest(TEST_EMAIL);
            performPost("/pre-signup/send-code", sendRequest)
                    .andExpect(status().isOk());

            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(TEST_EMAIL, "WRONG1");

            // when & then
            performPost("/pre-signup/verify-code", verifyRequest)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[PRE-004] 만료된 인증 코드 - 400 Bad Request")
        void verifyCode_withExpiredCode_returns400() throws Exception {
            // given - 만료된 코드 생성
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, "123456", 0);
            emailVerificationRepository.save(verification);

            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(TEST_EMAIL, "123456");

            // when & then
            performPost("/pre-signup/verify-code", verifyRequest)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[PRE-005] 5회 초과 인증 시도 - 429 Too Many Requests")
        void verifyCode_afterMaxAttempts_returns429() throws Exception {
            // given - 코드 발송
            ResendVerificationRequest sendRequest = new ResendVerificationRequest(TEST_EMAIL);
            performPost("/pre-signup/send-code", sendRequest)
                    .andExpect(status().isOk());

            EmailVerificationRequest wrongRequest = new EmailVerificationRequest(TEST_EMAIL, "WRONG1");
            for (int i = 0; i < MAX_VERIFICATION_ATTEMPTS; i++) {
                performPost("/pre-signup/verify-code", wrongRequest)
                        .andExpect(status().isBadRequest());
            }

            // when - 6번째 시도
            // then - 429 Too Many Requests
            performPost("/pre-signup/verify-code", wrongRequest)
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("[PRE-006] Rate Limit 적용 - 429 Too Many Requests")
        void sendCode_beforeRateLimitExpires_returns429() throws Exception {
            // given - 첫 번째 발송
            ResendVerificationRequest request = new ResendVerificationRequest(TEST_EMAIL);
            performPost("/pre-signup/send-code", request)
                    .andExpect(status().isOk());

            // when - 바로 재발송 시도 (rate limit 적용)
            // then
            performPost("/pre-signup/send-code", request)
                    .andExpect(status().isTooManyRequests());
        }
    }
}
