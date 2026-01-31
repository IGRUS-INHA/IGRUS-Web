package igrus.web.security.auth.password.controller;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

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
 *     <li>REG-001 ~ REG-003: 회원가입 성공 케이스</li>
 *     <li>REG-010 ~ REG-017: 회원가입 실패 케이스 (유효성 검증)</li>
 *     <li>VER-001 ~ VER-005: 이메일 인증 케이스</li>
 * </ul>
 */
@DisplayName("회원가입 HTTP 컨트롤러 통합 테스트")
class PasswordAuthControllerSignupIntegrationTest extends ControllerIntegrationTestBase {

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
                Gender.MALE,
                1,
                true
        );
    }

    private PasswordSignupRequest createSignupRequest(String studentId, String name, String email,
                                                       String password, String phone, String department,
                                                       String motivation, Boolean privacyConsent) {
        return new PasswordSignupRequest(
                studentId, name, email, password, phone, department, motivation, Gender.MALE, 1, privacyConsent
        );
    }

    // ===== 회원가입 성공 테스트 =====

    @Nested
    @DisplayName("회원가입 성공 테스트")
    class SignupSuccessTest {

        @Test
        @DisplayName("[REG-001] 회원가입 요청 성공 - 201 응답 및 이메일 인증 코드 발송")
        void signup_withValidRequest_returns201AndSendsVerificationEmail() throws Exception {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            performPost("/signup", request)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.requiresVerification").value(true));

            // 이메일 발송 확인
            verify(authEmailService).sendVerificationEmail(eq(TEST_EMAIL), anyString());

            // DB 상태 확인
            User savedUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
            assertThat(savedUser.getStudentId()).isEqualTo(TEST_STUDENT_ID);
        }

        @Test
        @DisplayName("[REG-002] 이메일 인증 코드 검증 성공 - 준회원 등록 완료")
        void verifyEmail_withValidCode_completesRegistration() throws Exception {
            // given - 회원가입 수행
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            performPost("/signup", signupRequest)
                    .andExpect(status().isCreated());

            // 인증 코드 조회
            String verificationCode = getVerificationCode(TEST_EMAIL);
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(TEST_EMAIL, verificationCode);

            // when & then
            performPost("/verify-email", verifyRequest)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requiresVerification").value(false));

            // DB 상태 확인
            User verifiedUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(verifiedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("[REG-003] 인증 코드 재발송 성공 - 새 코드 생성")
        void resendVerification_withValidEmail_sendsNewCode() throws Exception {
            // given - 회원가입 수행
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            performPost("/signup", signupRequest)
                    .andExpect(status().isCreated());

            // 기존 코드 저장
            String originalCode = getVerificationCode(TEST_EMAIL);

            // 재발송 rate limit 비활성화
            ReflectionTestUtils.setField(passwordSignupService, "resendRateLimitSeconds", 0L);

            ResendVerificationRequest resendRequest = new ResendVerificationRequest(TEST_EMAIL);

            // when & then
            performPost("/resend-verification", resendRequest)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.message").exists());

            // 새 코드가 발급되었는지 확인
            String newCode = getVerificationCode(TEST_EMAIL);
            assertThat(newCode).isNotEqualTo(originalCode);
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
        @DisplayName("[REG-013] 비밀번호 정책 미충족 (소문자 없음) - 400 Bad Request 응답")
        void signup_withPasswordMissingLowercase_returns400() throws Exception {
            // given
            PasswordSignupRequest request = createSignupRequest(
                    TEST_STUDENT_ID, TEST_NAME, TEST_EMAIL,
                    "TESTPASS1!@", // 소문자 없음
                    TEST_PHONE, TEST_DEPARTMENT,
                    TEST_MOTIVATION, true
            );

            // when & then
            performPost("/signup", request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[REG-014] 비밀번호 정책 미충족 (특수문자 없음) - 400 Bad Request 응답")
        void signup_withPasswordMissingSpecialChar_returns400() throws Exception {
            // given
            PasswordSignupRequest request = createSignupRequest(
                    TEST_STUDENT_ID, TEST_NAME, TEST_EMAIL,
                    "TestPass123", // 특수문자 없음
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

    // ===== 이메일 인증 테스트 =====

    @Nested
    @DisplayName("이메일 인증 테스트")
    class EmailVerificationTest {

        @Test
        @DisplayName("[VER-001] 만료된 인증 코드 사용 - 400 Bad Request 응답")
        void verifyEmail_withExpiredCode_returns400() throws Exception {
            // given - 회원가입 수행
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            performPost("/signup", signupRequest)
                    .andExpect(status().isCreated());

            // 인증 코드 만료 시뮬레이션
            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(TEST_EMAIL).orElseThrow();
            ReflectionTestUtils.setField(verification, "expiresAt", Instant.now().minusSeconds(60));
            emailVerificationRepository.save(verification);

            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(TEST_EMAIL, verification.getCode());

            // when & then
            performPost("/verify-email", verifyRequest)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[VER-002] 잘못된 인증 코드 사용 - 400 Bad Request 응답")
        void verifyEmail_withInvalidCode_returns400() throws Exception {
            // given - 회원가입 수행
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            performPost("/signup", signupRequest)
                    .andExpect(status().isCreated());

            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(TEST_EMAIL, "WRONG_CODE");

            // when & then
            performPost("/verify-email", verifyRequest)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[VER-003] 5회 초과 인증 시도 - 429 Too Many Requests 응답")
        void verifyEmail_afterMaxAttempts_returns429() throws Exception {
            // given - 회원가입 수행
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            performPost("/signup", signupRequest)
                    .andExpect(status().isCreated());

            // 실패 시도 횟수만큼 잘못된 인증 코드로 시도 (5회)
            EmailVerificationRequest wrongRequest = new EmailVerificationRequest(TEST_EMAIL, "WRONG1");
            for (int i = 0; i < MAX_VERIFICATION_ATTEMPTS; i++) {
                performPost("/verify-email", wrongRequest)
                        .andExpect(status().isBadRequest()); // 각 시도는 400 Bad Request (잘못된 코드)
            }

            // when - 6번째 시도
            // then - 429 Too Many Requests
            performPost("/verify-email", wrongRequest)
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("[VER-004] 존재하지 않는 이메일로 인증 시도 - 400 Bad Request 응답")
        void verifyEmail_withNonExistentEmail_returns400() throws Exception {
            // given
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest("nonexistent@inha.edu", "123456");

            // when & then
            performPost("/verify-email", verifyRequest)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[VER-005] 이미 인증된 이메일로 재인증 시도 - 400 Bad Request 응답")
        void verifyEmail_withAlreadyVerifiedEmail_returns400() throws Exception {
            // given - 회원가입 및 인증 완료
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            performPost("/signup", signupRequest)
                    .andExpect(status().isCreated());

            String verificationCode = getVerificationCode(TEST_EMAIL);
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(TEST_EMAIL, verificationCode);

            performPost("/verify-email", verifyRequest)
                    .andExpect(status().isOk());

            // when - 다시 인증 시도
            performPost("/verify-email", verifyRequest)
                    .andExpect(status().isBadRequest());
        }
    }

    // ===== 인증 코드 재발송 테스트 =====

    @Nested
    @DisplayName("인증 코드 재발송 테스트")
    class ResendVerificationTest {

        @Test
        @DisplayName("[RES-001] 재발송 Rate Limit 적용 - 429 Too Many Requests 응답")
        void resendVerification_beforeRateLimitExpires_returns429() throws Exception {
            // given - 회원가입 수행
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            performPost("/signup", signupRequest)
                    .andExpect(status().isCreated());

            ResendVerificationRequest resendRequest = new ResendVerificationRequest(TEST_EMAIL);

            // when - 바로 재발송 시도 (rate limit 적용)
            // then
            performPost("/resend-verification", resendRequest)
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("[RES-002] 존재하지 않는 이메일로 재발송 시도 - 200 OK (새 인증 코드 생성)")
        void resendVerification_withNonExistentEmail_returns200() throws Exception {
            // given - 현재 구현에서는 존재하지 않는 이메일도 새 인증 코드를 생성합니다
            String nonExistentEmail = "nonexistent@inha.edu";
            ResendVerificationRequest resendRequest = new ResendVerificationRequest(nonExistentEmail);

            // when & then - 새 인증 코드가 생성되어 성공 응답
            performPost("/resend-verification", resendRequest)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(nonExistentEmail))
                    .andExpect(jsonPath("$.message").exists());
        }
    }
}
