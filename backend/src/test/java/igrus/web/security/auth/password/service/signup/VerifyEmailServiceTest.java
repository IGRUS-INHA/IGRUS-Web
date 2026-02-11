package igrus.web.security.auth.password.service.signup;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.exception.verification.VerificationAttemptsExceededException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeExpiredException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeInvalidException;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.webhook.baebdungi.service.BaebdungiWebhookService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("VerifyEmailService 통합 테스트")
class VerifyEmailServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private SignupService signupService;

    @Autowired
    private VerifyEmailService verifyEmailService;

    @MockitoBean
    private AuthEmailService authEmailService;

    @MockitoBean
    private BaebdungiWebhookService baebdungiWebhookService;

    private static final String VALID_STUDENT_ID = "20231234";
    private static final String VALID_NAME = "홍길동";
    private static final String VALID_EMAIL = "test@inha.edu";
    private static final String VALID_PASSWORD = "testpass1";
    private static final String VALID_PHONE = "010-1234-5678";
    private static final String VALID_DEPARTMENT = "컴퓨터공학과";
    private static final String VALID_MOTIVATION = "프로그래밍을 배우고 싶습니다.";

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(signupService, "verificationCodeExpiry", 600000L);
        ReflectionTestUtils.setField(verifyEmailService, "maxAttempts", 5);
    }

    private PasswordSignupRequest createValidSignupRequest() {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                VALID_NAME,
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_PHONE,
                VALID_DEPARTMENT,
                VALID_MOTIVATION,
                List.of(),
                List.of(Interest.WEB_FRONTEND),
                null,
                JoinRoute.EVERYTIME,
                null,
                Gender.MALE,
                1,
                EnrollmentStatus.ENROLLED,
                true
        );
    }

    @Nested
    @DisplayName("이메일 인증")
    class VerifyEmailTest {

        @Test
        @DisplayName("10분 이내 올바른 인증 코드 입력 시 인증 완료 [REG-041]")
        void verifyEmail_WithValidCode_ReturnsSuccess() {
            // given - 회원가입을 통해 User, Credential, EmailVerification 생성
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            signupService.signup(signupRequest);

            // 생성된 인증 코드 조회
            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL).orElseThrow();
            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, verification.getCode());

            // when
            PasswordSignupResponse response = verifyEmailService.verifyEmail(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo(VALID_EMAIL);
            assertThat(response.requiresVerification()).isFalse();

            // DB에서 인증 상태 확인
            EmailVerification savedVerification = emailVerificationRepository.findById(verification.getId()).orElseThrow();
            assertThat(savedVerification.isVerified()).isTrue();
        }

        @Test
        @DisplayName("만료된 인증 코드 입력 시 만료 오류 [REG-042]")
        void verifyEmail_WithExpiredCode_ThrowsException() throws InterruptedException {
            // given: 만료 시간이 1ms인 인증 코드 생성 후, 확실한 만료를 위해 대기
            EmailVerification verification = EmailVerification.create(VALID_EMAIL, "123456", 1);
            emailVerificationRepository.save(verification);
            Thread.sleep(10); // 확실한 만료를 위해 10ms 대기

            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, "123456");

            // when & then
            assertThatThrownBy(() -> verifyEmailService.verifyEmail(request))
                    .isInstanceOf(VerificationCodeExpiredException.class);
        }

        @Test
        @DisplayName("잘못된 인증 코드 입력 시 오류 및 시도 횟수 증가 [REG-043]")
        void verifyEmail_WithWrongCode_ThrowsExceptionAndIncrementsAttempts() {
            // given
            EmailVerification verification = EmailVerification.create(VALID_EMAIL, "123456", 600000L);
            emailVerificationRepository.save(verification);

            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, "000000");

            // when & then
            assertThatThrownBy(() -> verifyEmailService.verifyEmail(request))
                    .isInstanceOf(VerificationCodeInvalidException.class);

            // DB에서 시도 횟수 확인
            EmailVerification savedVerification = emailVerificationRepository.findById(verification.getId()).orElseThrow();
            assertThat(savedVerification.getAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("5회 이상 잘못된 인증 코드 입력 시 차단 [REG-043]")
        void verifyEmail_ExceedsMaxAttempts_ThrowsException() {
            // given
            EmailVerification verification = EmailVerification.create(VALID_EMAIL, "123456", 600000L);
            // 5번 시도 누적
            for (int i = 0; i < 5; i++) {
                verification.incrementAttempts();
            }
            emailVerificationRepository.save(verification);

            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, "123456");

            // when & then
            assertThatThrownBy(() -> verifyEmailService.verifyEmail(request))
                    .isInstanceOf(VerificationAttemptsExceededException.class);
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 인증 시도 시 오류")
        void verifyEmail_WithNonExistentEmail_ThrowsException() {
            // given
            EmailVerificationRequest request = new EmailVerificationRequest("nonexistent@inha.edu", "123456");

            // when & then
            assertThatThrownBy(() -> verifyEmailService.verifyEmail(request))
                    .isInstanceOf(VerificationCodeInvalidException.class);
        }

        @Test
        @DisplayName("잘못된 인증 코드 입력 시 뱁둥이봇 웹훅이 호출되지 않는다")
        void verifyEmail_WithWrongCode_DoesNotCallBaebdungiWebhook() {
            // given
            EmailVerification verification = EmailVerification.create(VALID_EMAIL, "123456", 600000L);
            emailVerificationRepository.save(verification);

            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, "000000");

            // when & then
            assertThatThrownBy(() -> verifyEmailService.verifyEmail(request))
                    .isInstanceOf(VerificationCodeInvalidException.class);

            verify(baebdungiWebhookService, never()).sendSubmission(any(User.class));
        }
    }

    @Nested
    @DisplayName("이메일 인증 완료 시 사용자 상태 변경")
    class VerifyEmailUserStatusTest {

        @Test
        @DisplayName("이메일 인증 완료 시 User 상태가 ACTIVE로 변경")
        void verifyEmail_UserStatus_BecomesActive() {
            // given
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            signupService.signup(signupRequest);

            // 인증 코드 조회
            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL).orElseThrow();
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(VALID_EMAIL, verification.getCode());

            // when
            verifyEmailService.verifyEmail(verifyRequest);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(savedUser.isActive()).isTrue();
        }

        @Test
        @DisplayName("이메일 인증 완료 시 뱁둥이봇 웹훅이 호출된다")
        void verifyEmail_Success_CallsBaebdungiWebhook() {
            // given
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            signupService.signup(signupRequest);

            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL).orElseThrow();
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(VALID_EMAIL, verification.getCode());

            // when
            verifyEmailService.verifyEmail(verifyRequest);

            // then
            verify(baebdungiWebhookService).sendSubmission(any(User.class));
        }

        @Test
        @DisplayName("이메일 인증 완료 시 PasswordCredential 상태가 ACTIVE로 변경")
        void verifyEmail_CredentialStatus_BecomesActive() {
            // given
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            signupService.signup(signupRequest);

            // 인증 코드 조회
            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL).orElseThrow();
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(VALID_EMAIL, verification.getCode());

            // when
            verifyEmailService.verifyEmail(verifyRequest);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            PasswordCredential credential = passwordCredentialRepository.findByUserId(savedUser.getId()).orElseThrow();
            assertThat(credential.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(credential.isActive()).isTrue();
        }
    }
}
