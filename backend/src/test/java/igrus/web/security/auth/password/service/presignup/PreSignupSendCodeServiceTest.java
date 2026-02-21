package igrus.web.security.auth.password.service.presignup;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.verification.VerificationResendRateLimitedException;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.dto.response.VerificationResendResponse;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DisplayName("PreSignupSendCodeService 통합 테스트")
class PreSignupSendCodeServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private PreSignupSendCodeService preSignupSendCodeService;

    @MockitoBean
    private AuthEmailService authEmailService;

    private static final String VALID_EMAIL = "test@inha.edu";

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(preSignupSendCodeService, "verificationCodeExpiry", 600000L);
        ReflectionTestUtils.setField(preSignupSendCodeService, "resendRateLimitSeconds", 60L);
    }

    @Nested
    @DisplayName("코드 발송 성공")
    class SendCodeSuccessTest {

        @Test
        @DisplayName("새 이메일로 인증 코드 발송 성공")
        void sendCode_WithNewEmail_ReturnsSuccess() {
            // given
            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            // when
            VerificationResendResponse response = preSignupSendCodeService.sendCode(request);

            // then
            assertThat(response.email()).isEqualTo(VALID_EMAIL);
            assertThat(response.message()).isEqualTo("인증 코드가 발송되었습니다.");

            // DB에 인증 레코드 생성 확인
            Optional<EmailVerification> verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL);
            assertThat(verification).isPresent();
            assertThat(verification.get().getCode()).hasSize(6);
            assertThat(verification.get().getCode()).matches("^\\d{6}$");

            // 이메일 발송 확인
            verify(authEmailService).sendVerificationEmail(eq(VALID_EMAIL), anyString());
        }

        @Test
        @DisplayName("기존 미인증 레코드 삭제 후 새 레코드 생성")
        void sendCode_DeletesExistingUnverifiedRecord() {
            // given
            EmailVerification existing = EmailVerification.create(VALID_EMAIL, "111111", 600000L);
            emailVerificationRepository.save(existing);
            Long existingId = existing.getId();

            // rate limit 우회를 위해 createdAt을 과거로 native query로 설정
            transactionTemplate.execute(status -> {
                entityManager.createNativeQuery(
                                "UPDATE email_verifications SET email_verifications_created_at = :createdAt WHERE email_verifications_id = :id")
                        .setParameter("createdAt", java.time.Instant.now().minusSeconds(120))
                        .setParameter("id", existingId)
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
                return null;
            });

            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            // when
            preSignupSendCodeService.sendCode(request);

            // then
            assertThat(emailVerificationRepository.findById(existingId)).isEmpty();
            Optional<EmailVerification> newVerification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL);
            assertThat(newVerification).isPresent();
            assertThat(newVerification.get().getId()).isNotEqualTo(existingId);
        }
    }

    @Nested
    @DisplayName("코드 발송 실패")
    class SendCodeFailureTest {

        @Test
        @DisplayName("이미 ACTIVE 상태인 이메일로 발송 시 DuplicateEmailException")
        void sendCode_WithActiveEmail_ThrowsException() {
            // given
            createAndSaveUser("20231234", VALID_EMAIL, UserRole.ASSOCIATE);
            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            // when & then
            assertThatThrownBy(() -> preSignupSendCodeService.sendCode(request))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        @DisplayName("Rate limit 내 재요청 시 VerificationResendRateLimitedException")
        void sendCode_WithinRateLimit_ThrowsException() {
            // given - 첫 발송
            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);
            preSignupSendCodeService.sendCode(request);

            // when & then - 즉시 재발송
            assertThatThrownBy(() -> preSignupSendCodeService.sendCode(request))
                    .isInstanceOf(VerificationResendRateLimitedException.class);
        }
    }
}
