package igrus.web.security.auth.password.service.signup;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.signup.DuplicatePhoneNumberException;
import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DisplayName("SignupService 통합 테스트")
class SignupServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private SignupService signupService;

    @MockitoBean
    private AuthEmailService authEmailService;

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
                Gender.MALE,
                1,
                true
        );
    }

    @Nested
    @DisplayName("회원가입 - 필수 정보 검증")
    class SignupRequiredFieldsTest {

        @Test
        @DisplayName("모든 필수 정보 입력 시 회원가입 성공 및 인증 코드 발송 [REG-010]")
        void signup_WithAllRequiredFields_ReturnsSuccess() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            PasswordSignupResponse response = signupService.signup(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo(VALID_EMAIL);
            assertThat(response.requiresVerification()).isTrue();

            // 상태 검증 - DB에서 조회
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStudentId()).isEqualTo(VALID_STUDENT_ID);

            Optional<PasswordCredential> credential = passwordCredentialRepository.findByUserId(savedUser.getId());
            assertThat(credential).isPresent();

            List<PrivacyConsent> consents = privacyConsentRepository.findByUserIdOrderByConsentDateDesc(savedUser.getId());
            assertThat(consents).hasSize(1);

            Optional<EmailVerification> verification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL);
            assertThat(verification).isPresent();

            // 외부 의존성만 상호작용 검증
            verify(authEmailService).sendVerificationEmail(eq(VALID_EMAIL), anyString());
        }

        @Test
        @DisplayName("회원가입 시 6자리 인증 코드가 생성됨 [REG-040]")
        void signup_GeneratesSixDigitCode() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

            // when
            signupService.signup(request);

            // then
            verify(authEmailService).sendVerificationEmail(anyString(), codeCaptor.capture());
            String code = codeCaptor.getValue();
            assertThat(code).hasSize(6);
            assertThat(code).matches("^\\d{6}$");
        }

        @Test
        @DisplayName("회원가입 시 비밀번호가 BCrypt로 해시됨 [REG-026]")
        void signup_PasswordIsHashed() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then - DB에서 조회하여 검증
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            PasswordCredential credential = passwordCredentialRepository.findByUserId(savedUser.getId()).orElseThrow();

            // BCrypt 해시 패턴 확인 ($2a$ 또는 $2b$ 로 시작)
            assertThat(credential.getPasswordHash()).startsWith("$2");
            // 원본 비밀번호와 다름
            assertThat(credential.getPasswordHash()).isNotEqualTo(VALID_PASSWORD);
            // 비밀번호가 매칭되는지 확인
            assertThat(passwordEncoder.matches(VALID_PASSWORD, credential.getPasswordHash())).isTrue();
        }
    }

    @Nested
    @DisplayName("회원가입 - 중복 검사")
    class SignupDuplicationCheckTest {

        @Test
        @DisplayName("이미 가입된 학번으로 가입 시도 시 오류 [REG-030]")
        void signup_WithDuplicateStudentId_ThrowsException() {
            // given
            createAndSaveUser(VALID_STUDENT_ID, "other@inha.edu", igrus.web.user.domain.UserRole.ASSOCIATE);

            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(DuplicateStudentIdException.class);

            // 새 사용자가 저장되지 않았는지 확인
            assertThat(userRepository.findByEmail(VALID_EMAIL)).isEmpty();
        }

        @Test
        @DisplayName("이미 등록된 이메일로 가입 시도 시 오류 [REG-031]")
        void signup_WithDuplicateEmail_ThrowsException() {
            // given
            createAndSaveUser("99999999", VALID_EMAIL, igrus.web.user.domain.UserRole.ASSOCIATE);

            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(DuplicateEmailException.class);

            // 새 사용자가 저장되지 않았는지 확인
            assertThat(userRepository.findByStudentId(VALID_STUDENT_ID)).isEmpty();
        }

        @Test
        @DisplayName("이미 등록된 전화번호로 가입 시도 시 오류 [REG-032]")
        void signup_WithDuplicatePhoneNumber_ThrowsException() {
            // given
            User existingUser = User.create(
                    "99999999",
                    "기존사용자",
                    "other@inha.edu",
                    VALID_PHONE,
                    "기타학과",
                    "동기",
                    Gender.MALE,
                    1
            );
            userRepository.save(existingUser);

            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(DuplicatePhoneNumberException.class);

            // 새 사용자가 저장되지 않았는지 확인
            assertThat(userRepository.findByStudentId(VALID_STUDENT_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("회원가입 - 기존 인증 레코드 처리")
    class SignupExistingVerificationTest {

        @Test
        @DisplayName("회원가입 시 기존 미인증 이메일 인증 레코드 삭제")
        void signup_DeletesExistingUnverifiedRecord() {
            // given
            EmailVerification existingVerification = EmailVerification.create(VALID_EMAIL, "111111", 600000L);
            emailVerificationRepository.save(existingVerification);
            Long existingId = existingVerification.getId();

            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then - 기존 레코드는 삭제됨
            assertThat(emailVerificationRepository.findById(existingId)).isEmpty();

            // 새 레코드가 생성됨
            Optional<EmailVerification> newVerification = emailVerificationRepository.findByEmailAndVerifiedFalse(VALID_EMAIL);
            assertThat(newVerification).isPresent();
            assertThat(newVerification.get().getId()).isNotEqualTo(existingId);
        }
    }

    @Nested
    @DisplayName("회원가입 - 개인정보 동의")
    class SignupPrivacyConsentTest {

        @Test
        @DisplayName("회원가입 시 개인정보 동의 기록이 저장됨 [REG-004]")
        void signup_SavesPrivacyConsent() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then - DB에서 조회하여 검증
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            List<PrivacyConsent> consents = privacyConsentRepository.findByUserIdOrderByConsentDateDesc(savedUser.getId());

            assertThat(consents).hasSize(1);
            PrivacyConsent savedConsent = consents.get(0);
            assertThat(savedConsent.isConsentGiven()).isTrue();
            assertThat(savedConsent.getPolicyVersion()).isEqualTo("1.0");
        }
    }

    @Nested
    @DisplayName("회원가입 - User 엔티티 생성")
    class SignupUserCreationTest {

        @Test
        @DisplayName("회원가입 시 User 엔티티가 올바르게 생성됨")
        void signup_CreatesUserWithCorrectInfo() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then - DB에서 조회하여 검증
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStudentId()).isEqualTo(VALID_STUDENT_ID);
            assertThat(savedUser.getName()).isEqualTo(VALID_NAME);
            assertThat(savedUser.getEmail()).isEqualTo(VALID_EMAIL);
            assertThat(savedUser.getPhoneNumber()).isEqualTo(VALID_PHONE);
            assertThat(savedUser.getDepartment()).isEqualTo(VALID_DEPARTMENT);
            assertThat(savedUser.getMotivation()).isEqualTo(VALID_MOTIVATION);
        }
    }

    @Nested
    @DisplayName("회원가입 - 사용자 상태 관리")
    class SignupUserStatusTest {

        @Test
        @DisplayName("회원가입 시 User 상태가 PENDING_VERIFICATION")
        void signup_UserStatus_IsPendingVerification() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
            assertThat(savedUser.isPendingVerification()).isTrue();
        }

        @Test
        @DisplayName("회원가입 시 PasswordCredential 상태가 PENDING_VERIFICATION")
        void signup_CredentialStatus_IsPendingVerification() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            PasswordCredential credential = passwordCredentialRepository.findByUserId(savedUser.getId()).orElseThrow();
            assertThat(credential.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
            assertThat(credential.isPendingVerification()).isTrue();
        }
    }
}
