package igrus.web.security.auth.password.service.signup;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.common.exception.verification.VerificationEmailNotFoundException;
import igrus.web.security.auth.common.exception.verification.VerificationResendRateLimitedException;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.dto.response.VerificationResendResponse;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("ResendVerificationService 통합 테스트")
class ResendVerificationServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private ResendVerificationService resendVerificationService;

    @MockitoBean
    private AuthEmailService authEmailService;

    private static final String VALID_EMAIL = "test@inha.edu";
    private static final String VALID_STUDENT_ID = "20231234";

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(resendVerificationService, "verificationCodeExpiry", 600000L);
        ReflectionTestUtils.setField(resendVerificationService, "resendRateLimitSeconds", 300L);
    }

    @Nested
    @DisplayName("이메일 검증")
    class EmailValidationTest {

        @Test
        @DisplayName("가입 요청되지 않은 이메일로 재발송 시 오류 [REG-046]")
        void resendVerification_WithNonExistentEmail_ThrowsException() {
            // given
            ResendVerificationRequest request = new ResendVerificationRequest("unknown@inha.edu");

            // when & then
            assertThatThrownBy(() -> resendVerificationService.resendVerification(request))
                    .isInstanceOf(VerificationEmailNotFoundException.class);

            verify(authEmailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("이미 인증 완료된(ACTIVE) 이메일로 재발송 시 오류 [REG-047]")
        void resendVerification_WithActiveUserEmail_ThrowsException() {
            // given
            createAndSaveUser(VALID_STUDENT_ID, VALID_EMAIL, UserRole.ASSOCIATE);
            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            // when & then
            assertThatThrownBy(() -> resendVerificationService.resendVerification(request))
                    .isInstanceOf(VerificationEmailNotFoundException.class);

            verify(authEmailService, never()).sendVerificationEmail(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("인증 코드 재발송")
    class ResendVerificationTest {

        @Test
        @DisplayName("Rate Limit 시간 내 재발송 요청 시 Rate Limit 오류 [REG-044]")
        void resendVerification_WithinRateLimit_ThrowsException() {
            // given
            createAndSaveUnverifiedUser(VALID_STUDENT_ID, VALID_EMAIL, UserRole.ASSOCIATE);
            EmailVerification recentVerification = EmailVerification.create(VALID_EMAIL, "111111", 600000L);
            emailVerificationRepository.save(recentVerification);

            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            // when & then
            assertThatThrownBy(() -> resendVerificationService.resendVerification(request))
                    .isInstanceOf(VerificationResendRateLimitedException.class);

            verify(authEmailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("Rate Limit 경과 후 인증 코드 재발송 성공 [REG-045]")
        void resendVerification_AfterRateLimit_ReturnsSuccess() {
            // given
            createAndSaveUnverifiedUser(VALID_STUDENT_ID, VALID_EMAIL, UserRole.ASSOCIATE);
            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            // when
            VerificationResendResponse response = resendVerificationService.resendVerification(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo(VALID_EMAIL);
            assertThat(response.message()).isEqualTo("인증 코드가 재발송되었습니다.");

            // DB에서 새 인증 레코드 확인
            Optional<EmailVerification> verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL);
            assertThat(verification).isPresent();

            verify(authEmailService).sendVerificationEmail(eq(VALID_EMAIL), anyString());
        }

        @Test
        @DisplayName("재발송 시 기존 미인증 레코드 삭제 후 새 레코드 생성 [REG-045]")
        void resendVerification_DeletesOldRecord_CreatesNew() {
            // given
            createAndSaveUnverifiedUser(VALID_STUDENT_ID, VALID_EMAIL, UserRole.ASSOCIATE);
            EmailVerification oldVerification = EmailVerification.create(VALID_EMAIL, "111111", 600000L);
            emailVerificationRepository.save(oldVerification);

            // 오래된 레코드로 만들기 위해 저장 후 native query로 createdAt 업데이트
            // (@CreatedDate는 save() 시 덮어쓰므로 저장 후 업데이트 필요)
            Long oldVerificationId = oldVerification.getId();
            Instant pastTime = Instant.now().minusSeconds(400);
            transactionTemplate.execute(status -> {
                entityManager.createNativeQuery(
                        "UPDATE email_verifications SET email_verifications_created_at = :createdAt WHERE email_verifications_id = :id")
                        .setParameter("createdAt", pastTime)
                        .setParameter("id", oldVerificationId)
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
                return null;
            });

            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            // when
            resendVerificationService.resendVerification(request);

            // then - 기존 레코드는 삭제됨
            assertThat(emailVerificationRepository.findById(oldVerificationId)).isEmpty();

            // 새 레코드가 생성됨
            Optional<EmailVerification> newVerification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL);
            assertThat(newVerification).isPresent();
            assertThat(newVerification.get().getId()).isNotEqualTo(oldVerificationId);
        }

        @Test
        @DisplayName("재발송 시 새로운 6자리 인증 코드 생성 [REG-045]")
        void resendVerification_GeneratesNewCode() {
            // given
            createAndSaveUnverifiedUser(VALID_STUDENT_ID, VALID_EMAIL, UserRole.ASSOCIATE);
            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

            // when
            resendVerificationService.resendVerification(request);

            // then
            verify(authEmailService).sendVerificationEmail(anyString(), codeCaptor.capture());
            String code = codeCaptor.getValue();
            assertThat(code).hasSize(6);
            assertThat(code).matches("^\\d{6}$");
        }
    }
}
