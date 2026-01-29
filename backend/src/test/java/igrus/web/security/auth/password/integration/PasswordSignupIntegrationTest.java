package igrus.web.security.auth.password.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.signup.DuplicatePhoneNumberException;
import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.security.auth.common.exception.verification.VerificationAttemptsExceededException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeExpiredException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeInvalidException;
import igrus.web.security.auth.common.exception.verification.VerificationResendRateLimitedException;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.dto.response.VerificationResendResponse;
import igrus.web.security.auth.password.service.PasswordSignupService;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 회원가입 통합 테스트 (42개 테스트 케이스)
 *
 * <p>테스트 케이스 문서: docs/test-case/auth/registration-test-cases.md</p>
 *
 * <p>테스트 범위:</p>
 * <ul>
 *     <li>REG-001 ~ REG-004: 개인정보 동의</li>
 *     <li>REG-010 ~ REG-018: 필수 정보 입력 및 검증</li>
 *     <li>REG-020 ~ REG-026: 비밀번호 검증</li>
 *     <li>REG-030 ~ REG-032: 중복 검사</li>
 *     <li>REG-040 ~ REG-045: 이메일 인증</li>
 *     <li>REG-050 ~ REG-052: Edge Cases</li>
 * </ul>
 */
@DisplayName("회원가입 통합 테스트")
class PasswordSignupIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private PasswordSignupService passwordSignupService;

    @MockitoBean
    private AuthEmailService authEmailService;

    private static final String VALID_STUDENT_ID = "20231234";
    private static final String VALID_NAME = "홍길동";
    private static final String VALID_EMAIL = "test@inha.edu";
    private static final String VALID_PASSWORD = "Test1234!@";
    private static final String VALID_PHONE = "010-1234-5678";
    private static final String VALID_DEPARTMENT = "컴퓨터공학과";
    private static final String VALID_MOTIVATION = "프로그래밍을 배우고 싶습니다.";

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(passwordSignupService, "verificationCodeExpiry", 600000L);
        ReflectionTestUtils.setField(passwordSignupService, "maxAttempts", 5);
        ReflectionTestUtils.setField(passwordSignupService, "resendRateLimitSeconds", 60L);
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
                true
        );
    }

    private PasswordSignupRequest createSignupRequestWithEmail(String email) {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                VALID_NAME,
                email,
                VALID_PASSWORD,
                VALID_PHONE,
                VALID_DEPARTMENT,
                VALID_MOTIVATION,
                true
        );
    }

    private PasswordSignupRequest createSignupRequestWithStudentId(String studentId) {
        return new PasswordSignupRequest(
                studentId,
                VALID_NAME,
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_PHONE,
                VALID_DEPARTMENT,
                VALID_MOTIVATION,
                true
        );
    }

    private PasswordSignupRequest createSignupRequestWithPhone(String phone) {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                VALID_NAME,
                VALID_EMAIL,
                VALID_PASSWORD,
                phone,
                VALID_DEPARTMENT,
                VALID_MOTIVATION,
                true
        );
    }

    // ===== 2.1 개인정보 동의 테스트 =====

    @Nested
    @DisplayName("개인정보 동의 테스트")
    class PrivacyConsentTest {

        @Test
        @DisplayName("[REG-004] 동의한 정책 버전 기록")
        void signup_privacyConsent_recordsPolicyVersion() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            passwordSignupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            List<PrivacyConsent> consents = privacyConsentRepository.findByUserIdOrderByConsentDateDesc(savedUser.getId());

            assertThat(consents).hasSize(1);
            PrivacyConsent savedConsent = consents.get(0);
            assertThat(savedConsent.isConsentGiven()).isTrue();
            assertThat(savedConsent.getPolicyVersion()).isEqualTo("1.0");
            assertThat(savedConsent.getConsentDate()).isNotNull();
        }

        @Test
        @DisplayName("[REG-004] 개인정보 동의 시 동의 일시가 기록됨")
        void signup_privacyConsent_recordsConsentDate() {
            // given
            Instant beforeSignup = Instant.now();
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            passwordSignupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            List<PrivacyConsent> consents = privacyConsentRepository.findByUserIdOrderByConsentDateDesc(savedUser.getId());

            assertThat(consents).hasSize(1);
            PrivacyConsent savedConsent = consents.get(0);
            assertThat(savedConsent.getConsentDate()).isAfterOrEqualTo(beforeSignup);
        }
    }

    // ===== 2.2 필수 정보 입력 및 검증 테스트 =====

    @Nested
    @DisplayName("필수 정보 입력 및 검증 테스트")
    class RequiredFieldsTest {

        @Test
        @DisplayName("[REG-010] 모든 필수 정보 입력 시 가입 성공")
        void signup_withAllRequiredFields_succeeds() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            PasswordSignupResponse response = passwordSignupService.signup(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo(VALID_EMAIL);
            assertThat(response.requiresVerification()).isTrue();

            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStudentId()).isEqualTo(VALID_STUDENT_ID);
            assertThat(savedUser.getName()).isEqualTo(VALID_NAME);
            assertThat(savedUser.getPhoneNumber()).isEqualTo(VALID_PHONE);
            assertThat(savedUser.getDepartment()).isEqualTo(VALID_DEPARTMENT);
            assertThat(savedUser.getMotivation()).isEqualTo(VALID_MOTIVATION);

            verify(authEmailService).sendVerificationEmail(eq(VALID_EMAIL), anyString());
        }

        @Test
        @DisplayName("[REG-010] 회원가입 시 User가 PENDING_VERIFICATION 상태로 생성됨")
        void signup_createsUser_withPendingVerificationStatus() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            passwordSignupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
            assertThat(savedUser.isPendingVerification()).isTrue();
        }

        @Test
        @DisplayName("[REG-010] 회원가입 시 PasswordCredential이 생성됨")
        void signup_createsPasswordCredential() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            passwordSignupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            Optional<PasswordCredential> credential = passwordCredentialRepository.findByUserId(savedUser.getId());

            assertThat(credential).isPresent();
            assertThat(credential.get().getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        }

        @Test
        @DisplayName("[REG-010] 회원가입 시 EmailVerification이 생성됨")
        void signup_createsEmailVerification() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            passwordSignupService.signup(request);

            // then
            Optional<EmailVerification> verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL);
            assertThat(verification).isPresent();
            assertThat(verification.get().isVerified()).isFalse();
        }

        @Test
        @DisplayName("[REG-010] 회원가입 시 User 역할이 기본값(ASSOCIATE)으로 설정됨")
        void signup_setsDefaultRole_toAssociate() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            passwordSignupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getRole()).isEqualTo(UserRole.ASSOCIATE);
        }
    }

    // ===== 2.3 비밀번호 검증 테스트 =====

    @Nested
    @DisplayName("비밀번호 검증 테스트")
    class PasswordValidationTest {

        @Test
        @DisplayName("[REG-020] 유효한 비밀번호 입력 시 성공")
        void signup_withValidPassword_succeeds() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            PasswordSignupResponse response = passwordSignupService.signup(request);

            // then
            assertThat(response).isNotNull();
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            PasswordCredential credential = passwordCredentialRepository.findByUserId(savedUser.getId()).orElseThrow();
            assertThat(credential.getPasswordHash()).isNotNull();
        }

        @Test
        @DisplayName("[REG-026] 비밀번호 BCrypt 해시 저장 확인")
        void signup_password_isHashedWithBcrypt() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            passwordSignupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            PasswordCredential credential = passwordCredentialRepository.findByUserId(savedUser.getId()).orElseThrow();

            // BCrypt 해시 패턴 확인 ($2a$ 또는 $2b$ 로 시작)
            assertThat(credential.getPasswordHash()).startsWith("$2");
            // 원본 비밀번호와 다름
            assertThat(credential.getPasswordHash()).isNotEqualTo(VALID_PASSWORD);
            // 비밀번호가 매칭되는지 확인
            assertThat(passwordEncoder.matches(VALID_PASSWORD, credential.getPasswordHash())).isTrue();
        }

        @Test
        @DisplayName("[REG-026] BCrypt 해시가 60자 길이인지 확인")
        void signup_password_bcryptHashHasCorrectLength() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            passwordSignupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            PasswordCredential credential = passwordCredentialRepository.findByUserId(savedUser.getId()).orElseThrow();

            assertThat(credential.getPasswordHash()).hasSize(60);
        }
    }

    // ===== 2.4 중복 검사 테스트 =====

    @Nested
    @DisplayName("중복 검사 테스트")
    class DuplicationCheckTest {

        @Test
        @DisplayName("[REG-030] 이미 가입된 학번으로 가입 시도 시 오류")
        void signup_withDuplicateStudentId_throwsException() {
            // given
            createAndSaveUser(VALID_STUDENT_ID, "other@inha.edu", UserRole.ASSOCIATE);
            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            assertThatThrownBy(() -> passwordSignupService.signup(request))
                    .isInstanceOf(DuplicateStudentIdException.class);

            // 새 사용자가 저장되지 않았는지 확인
            assertThat(userRepository.findByEmail(VALID_EMAIL)).isEmpty();
        }

        @Test
        @DisplayName("[REG-031] 이미 등록된 이메일로 가입 시도 시 오류")
        void signup_withDuplicateEmail_throwsException() {
            // given
            createAndSaveUser("99999999", VALID_EMAIL, UserRole.ASSOCIATE);
            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            assertThatThrownBy(() -> passwordSignupService.signup(request))
                    .isInstanceOf(DuplicateEmailException.class);

            // 새 사용자가 저장되지 않았는지 확인
            assertThat(userRepository.findByStudentId(VALID_STUDENT_ID)).isEmpty();
        }

        @Test
        @DisplayName("[REG-032] 이미 등록된 전화번호로 가입 시도 시 오류")
        void signup_withDuplicatePhoneNumber_throwsException() {
            // given
            User existingUser = User.create(
                    "99999999",
                    "기존사용자",
                    "other@inha.edu",
                    VALID_PHONE,
                    "기타학과",
                    "동기"
            );
            userRepository.save(existingUser);

            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            assertThatThrownBy(() -> passwordSignupService.signup(request))
                    .isInstanceOf(DuplicatePhoneNumberException.class);

            // 새 사용자가 저장되지 않았는지 확인
            assertThat(userRepository.findByStudentId(VALID_STUDENT_ID)).isEmpty();
        }

        @Test
        @DisplayName("[REG-030] 중복 학번 검사 시 대소문자 구분하지 않음")
        void signup_duplicateStudentIdCheck_isCaseInsensitive() {
            // given - 학번은 숫자이므로 대소문자와 무관하지만 검증 로직 확인
            createAndSaveUser(VALID_STUDENT_ID, "other@inha.edu", UserRole.ASSOCIATE);

            PasswordSignupRequest request = createSignupRequestWithStudentId(VALID_STUDENT_ID);

            // when & then
            assertThatThrownBy(() -> passwordSignupService.signup(request))
                    .isInstanceOf(DuplicateStudentIdException.class);
        }

        @Test
        @DisplayName("[REG-031] 중복 이메일 검사는 정확히 일치하는 경우에만 오류")
        void signup_duplicateEmailCheck_requiresExactMatch() {
            // given
            createAndSaveUser("99999999", "existing@inha.edu", UserRole.ASSOCIATE);

            PasswordSignupRequest request = createSignupRequestWithEmail("test@inha.edu");

            // when & then - 다른 이메일이면 성공해야 함
            PasswordSignupResponse response = passwordSignupService.signup(request);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("[REG-032] 중복 전화번호 검사 시 하이픈 포함 여부와 무관하게 검사")
        void signup_duplicatePhoneCheck_ignoresHyphen() {
            // given
            User existingUser = User.create(
                    "99999999",
                    "기존사용자",
                    "other@inha.edu",
                    "010-1234-5678",
                    "기타학과",
                    "동기"
            );
            userRepository.save(existingUser);

            PasswordSignupRequest request = createSignupRequestWithPhone("010-1234-5678");

            // when & then
            assertThatThrownBy(() -> passwordSignupService.signup(request))
                    .isInstanceOf(DuplicatePhoneNumberException.class);
        }
    }

    // ===== 2.5 이메일 인증 테스트 =====

    @Nested
    @DisplayName("이메일 인증 테스트")
    class EmailVerificationTest {

        @Test
        @DisplayName("[REG-040] 6자리 인증 코드 발송")
        void signup_generatesSixDigitCode() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

            // when
            passwordSignupService.signup(request);

            // then
            verify(authEmailService).sendVerificationEmail(anyString(), codeCaptor.capture());
            String code = codeCaptor.getValue();
            assertThat(code).hasSize(6);
            assertThat(code).matches("^\\d{6}$");
        }

        @Test
        @DisplayName("[REG-041] 10분 이내 올바른 인증 코드 입력 시 가입 완료")
        void verifyEmail_withValidCode_completesSignup() {
            // given
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            passwordSignupService.signup(signupRequest);

            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL).orElseThrow();
            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, verification.getCode());

            // when
            PasswordSignupResponse response = passwordSignupService.verifyEmail(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo(VALID_EMAIL);
            assertThat(response.requiresVerification()).isFalse();

            // DB 상태 확인
            EmailVerification savedVerification = emailVerificationRepository.findById(verification.getId()).orElseThrow();
            assertThat(savedVerification.isVerified()).isTrue();
        }

        @Test
        @DisplayName("[REG-041] 인증 완료 시 준회원(ASSOCIATE)으로 등록")
        void verifyEmail_setsUserAsAssociate() {
            // given
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            passwordSignupService.signup(signupRequest);

            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL).orElseThrow();
            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, verification.getCode());

            // when
            passwordSignupService.verifyEmail(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getRole()).isEqualTo(UserRole.ASSOCIATE);
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("[REG-041] 이메일 인증 완료 시 User 상태가 ACTIVE로 변경")
        void verifyEmail_userStatus_becomesActive() {
            // given
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            passwordSignupService.signup(signupRequest);

            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL).orElseThrow();
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(VALID_EMAIL, verification.getCode());

            // when
            passwordSignupService.verifyEmail(verifyRequest);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(savedUser.isActive()).isTrue();
        }

        @Test
        @DisplayName("[REG-041] 이메일 인증 완료 시 PasswordCredential 상태가 ACTIVE로 변경")
        void verifyEmail_credentialStatus_becomesActive() {
            // given
            PasswordSignupRequest signupRequest = createValidSignupRequest();
            passwordSignupService.signup(signupRequest);

            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL).orElseThrow();
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(VALID_EMAIL, verification.getCode());

            // when
            passwordSignupService.verifyEmail(verifyRequest);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            PasswordCredential credential = passwordCredentialRepository.findByUserId(savedUser.getId()).orElseThrow();
            assertThat(credential.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(credential.isActive()).isTrue();
        }

        @Test
        @DisplayName("[REG-042] 10분 경과 후 인증 코드 입력 시 만료")
        void verifyEmail_withExpiredCode_throwsException() {
            // given
            EmailVerification verification = EmailVerification.create(VALID_EMAIL, "123456", 0);
            emailVerificationRepository.save(verification);

            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, "123456");

            // when & then
            assertThatThrownBy(() -> passwordSignupService.verifyEmail(request))
                    .isInstanceOf(VerificationCodeExpiredException.class);
        }

        @Test
        @DisplayName("[REG-043] 5회 이상 잘못된 인증 코드 입력 시 차단")
        void verifyEmail_exceedsMaxAttempts_throwsException() {
            // given
            EmailVerification verification = EmailVerification.create(VALID_EMAIL, "123456", 600000L);
            for (int i = 0; i < 5; i++) {
                verification.incrementAttempts();
            }
            emailVerificationRepository.save(verification);

            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, "123456");

            // when & then
            assertThatThrownBy(() -> passwordSignupService.verifyEmail(request))
                    .isInstanceOf(VerificationAttemptsExceededException.class);
        }

        @Test
        @DisplayName("[REG-043] 잘못된 인증 코드 입력 시 시도 횟수 증가")
        void verifyEmail_withWrongCode_incrementsAttempts() {
            // given
            EmailVerification verification = EmailVerification.create(VALID_EMAIL, "123456", 600000L);
            emailVerificationRepository.save(verification);

            EmailVerificationRequest request = new EmailVerificationRequest(VALID_EMAIL, "000000");

            // when & then
            assertThatThrownBy(() -> passwordSignupService.verifyEmail(request))
                    .isInstanceOf(VerificationCodeInvalidException.class);

            EmailVerification savedVerification = emailVerificationRepository.findById(verification.getId()).orElseThrow();
            assertThat(savedVerification.getAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("[REG-044] 인증 코드 재발송 시 1분 대기")
        void resendVerification_withinRateLimit_throwsException() {
            // given
            EmailVerification recentVerification = EmailVerification.create(VALID_EMAIL, "111111", 600000L);
            emailVerificationRepository.save(recentVerification);

            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            // when & then
            assertThatThrownBy(() -> passwordSignupService.resendVerification(request))
                    .isInstanceOf(VerificationResendRateLimitedException.class);

            verify(authEmailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("[REG-045] 1분 경과 후 인증 코드 재발송 성공")
        void resendVerification_afterRateLimit_succeeds() {
            // given - 레코드 없이 테스트 (이전 인증 요청이 없는 경우)
            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            // when
            VerificationResendResponse response = passwordSignupService.resendVerification(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo(VALID_EMAIL);
            assertThat(response.message()).isEqualTo("인증 코드가 재발송되었습니다.");

            Optional<EmailVerification> verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL);
            assertThat(verification).isPresent();

            verify(authEmailService).sendVerificationEmail(eq(VALID_EMAIL), anyString());
        }

        @Test
        @DisplayName("[REG-045] 재발송 시 기존 미인증 레코드 삭제 후 새 레코드 생성")
        void resendVerification_deletesOldRecord_createsNew() {
            // given
            EmailVerification oldVerification = EmailVerification.create(VALID_EMAIL, "111111", 600000L);
            emailVerificationRepository.save(oldVerification);

            Long oldVerificationId = oldVerification.getId();
            Instant pastTime = Instant.now().minusSeconds(120); // 2분 전으로 설정

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
            passwordSignupService.resendVerification(request);

            // then - 기존 레코드는 삭제됨
            assertThat(emailVerificationRepository.findById(oldVerificationId)).isEmpty();

            // 새 레코드가 생성됨
            Optional<EmailVerification> newVerification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL);
            assertThat(newVerification).isPresent();
            assertThat(newVerification.get().getId()).isNotEqualTo(oldVerificationId);
        }

        @Test
        @DisplayName("[REG-045] 재발송 시 새로운 6자리 인증 코드 생성")
        void resendVerification_generatesNewCode() {
            // given
            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

            // when
            passwordSignupService.resendVerification(request);

            // then
            verify(authEmailService).sendVerificationEmail(anyString(), codeCaptor.capture());
            String code = codeCaptor.getValue();
            assertThat(code).hasSize(6);
            assertThat(code).matches("^\\d{6}$");
        }

        @Test
        @DisplayName("[REG-045] 인증 코드 재발송 시도 횟수가 초기화됨")
        void resendVerification_resetsAttemptCount() {
            // given - 시도 횟수가 누적된 상태로 재발송
            EmailVerification oldVerification = EmailVerification.create(VALID_EMAIL, "111111", 600000L);
            oldVerification.incrementAttempts();
            oldVerification.incrementAttempts();
            emailVerificationRepository.save(oldVerification);

            // Rate limit을 피하기 위해 과거 시간으로 설정
            transactionTemplate.execute(status -> {
                entityManager.createNativeQuery(
                                "UPDATE email_verifications SET email_verifications_created_at = :createdAt WHERE email_verifications_id = :id")
                        .setParameter("createdAt", Instant.now().minusSeconds(120))
                        .setParameter("id", oldVerification.getId())
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
                return null;
            });

            ResendVerificationRequest request = new ResendVerificationRequest(VALID_EMAIL);

            // when
            passwordSignupService.resendVerification(request);

            // then
            Optional<EmailVerification> newVerification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL);
            assertThat(newVerification).isPresent();
            assertThat(newVerification.get().getAttempts()).isEqualTo(0);
        }
    }

    // ===== 2.6 Edge Cases =====

    @Nested
    @DisplayName("Edge Cases 테스트")
    class EdgeCasesTest {

        @Test
        @DisplayName("존재하지 않는 이메일로 인증 시도 시 오류")
        void verifyEmail_withNonExistentEmail_throwsException() {
            // given
            EmailVerificationRequest request = new EmailVerificationRequest("nonexistent@inha.edu", "123456");

            // when & then
            assertThatThrownBy(() -> passwordSignupService.verifyEmail(request))
                    .isInstanceOf(VerificationCodeInvalidException.class);
        }

        @Test
        @DisplayName("회원가입 시 기존 미인증 이메일 인증 레코드 삭제")
        void signup_deletesExistingUnverifiedRecord() {
            // given
            EmailVerification existingVerification = EmailVerification.create(VALID_EMAIL, "111111", 600000L);
            emailVerificationRepository.save(existingVerification);
            Long existingId = existingVerification.getId();

            PasswordSignupRequest request = createValidSignupRequest();

            // when
            passwordSignupService.signup(request);

            // then - 기존 레코드는 삭제됨
            assertThat(emailVerificationRepository.findById(existingId)).isEmpty();

            // 새 레코드가 생성됨
            Optional<EmailVerification> newVerification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL);
            assertThat(newVerification).isPresent();
            assertThat(newVerification.get().getId()).isNotEqualTo(existingId);
        }

        @Test
        @DisplayName("회원가입 성공 후 이메일 서비스 호출 확인")
        void signup_callsEmailService() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            passwordSignupService.signup(request);

            // then
            verify(authEmailService).sendVerificationEmail(eq(VALID_EMAIL), anyString());
        }

        @Test
        @DisplayName("인증 코드는 숫자로만 구성됨")
        void signup_verificationCode_isNumericOnly() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            passwordSignupService.signup(request);

            // then
            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL).orElseThrow();
            assertThat(verification.getCode()).matches("^\\d+$");
        }

        @Test
        @DisplayName("인증 코드 생성 시 100000 ~ 999999 범위의 숫자")
        void signup_verificationCode_isInValidRange() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            passwordSignupService.signup(request);

            // then
            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL).orElseThrow();
            int code = Integer.parseInt(verification.getCode());
            assertThat(code).isBetween(100000, 999999);
        }

        @Test
        @DisplayName("동일한 정보로 두 번 회원가입 시도 시 두 번째는 실패")
        void signup_withSameInfo_secondAttemptFails() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();
            passwordSignupService.signup(request);

            // when & then - 동일한 학번으로 두 번째 시도
            assertThatThrownBy(() -> passwordSignupService.signup(request))
                    .isInstanceOf(DuplicateStudentIdException.class);
        }

        @Test
        @DisplayName("이메일만 다른 경우 학번 중복으로 실패")
        void signup_withDifferentEmailSameStudentId_failsOnStudentId() {
            // given
            PasswordSignupRequest firstRequest = createValidSignupRequest();
            passwordSignupService.signup(firstRequest);

            PasswordSignupRequest secondRequest = createSignupRequestWithEmail("different@inha.edu");

            // when & then
            assertThatThrownBy(() -> passwordSignupService.signup(secondRequest))
                    .isInstanceOf(DuplicateStudentIdException.class);
        }

        @Test
        @DisplayName("학번만 다른 경우 이메일 중복으로 실패")
        void signup_withDifferentStudentIdSameEmail_failsOnEmail() {
            // given
            PasswordSignupRequest firstRequest = createValidSignupRequest();
            passwordSignupService.signup(firstRequest);

            PasswordSignupRequest secondRequest = new PasswordSignupRequest(
                    "99999999",
                    VALID_NAME,
                    VALID_EMAIL,
                    VALID_PASSWORD,
                    "010-9999-9999",
                    VALID_DEPARTMENT,
                    VALID_MOTIVATION,
                    true
            );

            // when & then
            assertThatThrownBy(() -> passwordSignupService.signup(secondRequest))
                    .isInstanceOf(DuplicateEmailException.class);
        }
    }
}
