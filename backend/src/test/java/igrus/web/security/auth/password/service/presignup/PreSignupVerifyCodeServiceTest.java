package igrus.web.security.auth.password.service.presignup;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.exception.verification.VerificationAttemptsExceededException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeExpiredException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeInvalidException;
import igrus.web.security.auth.password.dto.response.PreSignupVerificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PreSignupVerifyCodeService 통합 테스트")
class PreSignupVerifyCodeServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private PreSignupVerifyCodeService preSignupVerifyCodeService;

    private static final String VALID_EMAIL = "test@inha.edu";
    private static final String VALID_CODE = "123456";
    private static final int MAX_ATTEMPTS = 5;

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(preSignupVerifyCodeService, "maxAttempts", MAX_ATTEMPTS);
    }

    private EmailVerification createAndSaveVerification(String email, String code) {
        EmailVerification verification = EmailVerification.create(email, code, 600000L);
        return emailVerificationRepository.save(verification);
    }

    @Nested
    @DisplayName("인증 코드 확인 성공")
    class VerifyCodeSuccessTest {

        @Test
        @DisplayName("올바른 인증 코드 입력 시 인증 완료")
        void verifyCode_WithValidCode_ReturnsSuccess() {
            // given
            createAndSaveVerification(VALID_EMAIL, VALID_CODE);
            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, VALID_CODE);

            // when
            PreSignupVerificationResponse response = preSignupVerifyCodeService.verifyCode(request);

            // then
            assertThat(response.email()).isEqualTo(VALID_EMAIL);
            assertThat(response.verified()).isTrue();
            assertThat(response.message()).isEqualTo("이메일 인증이 완료되었습니다.");
            assertThat(response.verificationToken()).isNotNull();
            assertThat(response.verificationToken()).hasSize(36); // UUID format

            // DB에서 verified=true 확인
            assertThat(emailVerificationRepository.existsByEmailAndVerifiedTrue(VALID_EMAIL)).isTrue();
        }
    }

    @Nested
    @DisplayName("인증 코드 확인 실패")
    class VerifyCodeFailureTest {

        @Test
        @DisplayName("존재하지 않는 이메일로 인증 시도 시 VerificationCodeInvalidException")
        void verifyCode_WithNonExistentEmail_ThrowsException() {
            // given
            EmailVerificationRequest request = new EmailVerificationRequest("nonexistent@inha.edu", VALID_CODE);

            // when & then
            assertThatThrownBy(() -> preSignupVerifyCodeService.verifyCode(request))
                    .isInstanceOf(VerificationCodeInvalidException.class);
        }

        @Test
        @DisplayName("만료된 인증 코드 입력 시 VerificationCodeExpiredException")
        void verifyCode_WithExpiredCode_ThrowsException() {
            // given - 만료된 레코드 생성 (expiryMillis = 0)
            EmailVerification verification = EmailVerification.create(VALID_EMAIL, VALID_CODE, 0L);
            emailVerificationRepository.save(verification);

            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, VALID_CODE);

            // when & then
            assertThatThrownBy(() -> preSignupVerifyCodeService.verifyCode(request))
                    .isInstanceOf(VerificationCodeExpiredException.class);
        }

        @Test
        @DisplayName("잘못된 인증 코드 입력 시 VerificationCodeInvalidException 및 시도 횟수 증가")
        void verifyCode_WithWrongCode_ThrowsExceptionAndIncrementsAttempts() {
            // given
            createAndSaveVerification(VALID_EMAIL, VALID_CODE);
            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, "000000");

            // when & then
            assertThatThrownBy(() -> preSignupVerifyCodeService.verifyCode(request))
                    .isInstanceOf(VerificationCodeInvalidException.class);

            // 시도 횟수 증가 확인
            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL).orElseThrow();
            assertThat(verification.getAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("최대 시도 횟수 초과 시 VerificationAttemptsExceededException")
        void verifyCode_WithExceededAttempts_ThrowsException() {
            // given - 시도 횟수를 최대까지 채움
            EmailVerification verification = createAndSaveVerification(VALID_EMAIL, VALID_CODE);
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                verification.incrementAttempts();
            }
            emailVerificationRepository.save(verification);

            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, VALID_CODE);

            // when & then
            assertThatThrownBy(() -> preSignupVerifyCodeService.verifyCode(request))
                    .isInstanceOf(VerificationAttemptsExceededException.class);
        }
    }
}
