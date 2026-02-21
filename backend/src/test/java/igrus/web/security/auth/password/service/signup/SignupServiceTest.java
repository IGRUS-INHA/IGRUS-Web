package igrus.web.security.auth.password.service.signup;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.signup.DuplicatePhoneNumberException;
import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.security.auth.common.exception.signup.VerificationTokenInvalidException;
import igrus.web.security.auth.common.exception.signup.InvalidCustomFieldException;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.Interest;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import igrus.web.webhook.baebdungi.service.BaebdungiWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@DisplayName("SignupService 통합 테스트")
class SignupServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private SignupService signupService;

    @Autowired
    private BaebdungiWebhookService baebdungiWebhookService;

    private static final String VALID_STUDENT_ID = "20231234";
    private static final String VALID_NAME = "홍길동";
    private static final String VALID_EMAIL = "test@inha.edu";
    private static final String VALID_PASSWORD = "testpass1";
    private static final String VALID_PHONE = "010-1234-5678";
    private static final String VALID_DEPARTMENT = "컴퓨터공학과";
    private static final String VALID_MOTIVATION = "프로그래밍을 배우고 싶습니다.";

    private String verificationToken;

    @BeforeEach
    void setUp() {
        setUpBase();
        Mockito.reset(baebdungiWebhookService);
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
                true,
                verificationToken
        );
    }

    /**
     * 사전 이메일 인증 완료 레코드를 생성합니다.
     */
    private void createVerifiedEmailRecord(String email) {
        EmailVerification verification = EmailVerification.create(email, "123456", 600000L);
        this.verificationToken = verification.verify();
        emailVerificationRepository.save(verification);
    }

    @Nested
    @DisplayName("회원가입 - 이메일 사전 인증 필수")
    class SignupEmailVerificationRequiredTest {

        @Test
        @DisplayName("이메일 사전 인증 없이 가입 시도 시 EmailVerificationRequiredException")
        void signup_WithoutEmailVerification_ThrowsException() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(VerificationTokenInvalidException.class);

            // 사용자가 생성되지 않음
            assertThat(userRepository.findByEmail(VALID_EMAIL)).isEmpty();
        }

        @Test
        @DisplayName("미인증(verified=false) 레코드만 있는 경우 가입 거부")
        void signup_WithUnverifiedEmailRecord_ThrowsException() {
            // given
            EmailVerification unverified = EmailVerification.create(VALID_EMAIL, "123456", 600000L);
            emailVerificationRepository.save(unverified);

            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(VerificationTokenInvalidException.class);
        }
    }

    @Nested
    @DisplayName("회원가입 - 필수 정보 검증")
    class SignupRequiredFieldsTest {

        @Test
        @DisplayName("모든 필수 정보 입력 시 회원가입 성공 [REG-010]")
        void signup_WithAllRequiredFields_ReturnsSuccess() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            PasswordSignupResponse response = signupService.signup(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo(VALID_EMAIL);

            // 상태 검증 - DB에서 조회
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStudentId()).isEqualTo(VALID_STUDENT_ID);
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

            PasswordCredential credential = passwordCredentialRepository.findByUserId(savedUser.getId()).orElseThrow();
            assertThat(credential.getStatus()).isEqualTo(UserStatus.ACTIVE);

            List<PrivacyConsent> consents = privacyConsentRepository.findByUserIdOrderByConsentDateDesc(savedUser.getId());
            assertThat(consents).hasSize(1);

            // 인증 레코드 정리 확인
            assertThat(emailVerificationRepository.existsByEmailAndVerifiedTrue(VALID_EMAIL)).isFalse();

            // 웹훅 호출 확인
            verify(baebdungiWebhookService).sendSubmission(any(User.class));
        }

        @Test
        @DisplayName("회원가입 시 비밀번호가 BCrypt로 해시됨 [REG-026]")
        void signup_PasswordIsHashed() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
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
            createAndSaveUser(VALID_STUDENT_ID, "other@inha.edu", UserRole.ASSOCIATE);
            createVerifiedEmailRecord(VALID_EMAIL);

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
            createAndSaveUser("99999999", VALID_EMAIL, UserRole.ASSOCIATE);
            createVerifiedEmailRecord(VALID_EMAIL);

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
                    "99999999", "기존사용자", "other@inha.edu", VALID_PHONE,
                    "기타학과", "동기", List.of(), Gender.MALE, 1,
                    EnrollmentStatus.ENROLLED, List.of(), null, null, null
            );
            userRepository.save(existingUser);
            createVerifiedEmailRecord(VALID_EMAIL);

            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(DuplicatePhoneNumberException.class);

            // 새 사용자가 저장되지 않았는지 확인
            assertThat(userRepository.findByStudentId(VALID_STUDENT_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("회원가입 - 사용자 상태 관리")
    class SignupUserStatusTest {

        @Test
        @DisplayName("회원가입 시 User 상태가 ACTIVE")
        void signup_UserStatus_IsActive() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("회원가입 시 PasswordCredential 상태가 ACTIVE")
        void signup_CredentialStatus_IsActive() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            PasswordCredential credential = passwordCredentialRepository.findByUserId(savedUser.getId()).orElseThrow();
            assertThat(credential.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("회원가입 - 개인정보 동의")
    class SignupPrivacyConsentTest {

        @Test
        @DisplayName("회원가입 시 개인정보 동의 기록이 저장됨 [REG-004]")
        void signup_SavesPrivacyConsent() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then
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
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStudentId()).isEqualTo(VALID_STUDENT_ID);
            assertThat(savedUser.getName()).isEqualTo(VALID_NAME);
            assertThat(savedUser.getEmail()).isEqualTo(VALID_EMAIL);
            assertThat(savedUser.getPhoneNumber()).isEqualTo(VALID_PHONE);
            assertThat(savedUser.getDepartment()).isEqualTo(VALID_DEPARTMENT);
            assertThat(savedUser.getMotivation()).isEqualTo(VALID_MOTIVATION);
        }
    }

    // === OTHER 교차 검증 테스트 (INT-INV-03, INT-INV-04) ===

    @Nested
    @DisplayName("회원가입 - OTHER 교차 검증 (관심 분야, INT-INV-03)")
    class SignupInterestOtherValidationTest {

        @Test
        @DisplayName("OTHER 포함 + customInterest null → 예외 [SINT-030]")
        void signup_WithOtherInterestWithoutCustom_ThrowsException() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    List.of(Interest.AI, Interest.OTHER), null,
                    JoinRoute.EVERYTIME, null,
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(InvalidCustomFieldException.class);
        }

        @Test
        @DisplayName("OTHER 포함 + customInterest 빈 문자열 → 예외 [SINT-031]")
        void signup_WithOtherInterestWithBlankCustom_ThrowsException() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    List.of(Interest.OTHER), "",
                    JoinRoute.EVERYTIME, null,
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(InvalidCustomFieldException.class);
        }

        @Test
        @DisplayName("OTHER 포함 + customInterest 공백만 → 예외 [SINT-032]")
        void signup_WithOtherInterestWithWhitespaceCustom_ThrowsException() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    List.of(Interest.OTHER), "   ",
                    JoinRoute.EVERYTIME, null,
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(InvalidCustomFieldException.class);
        }

        @Test
        @DisplayName("OTHER 포함 + customInterest 유효 → 성공 [SINT-033]")
        void signup_WithOtherInterestAndCustom_Succeeds() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    List.of(Interest.AI, Interest.OTHER), "임베디드 시스템",
                    JoinRoute.EVERYTIME, null,
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );

            // when
            PasswordSignupResponse response = signupService.signup(request);

            // then
            assertThat(response).isNotNull();
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getInterests()).contains(Interest.AI, Interest.OTHER);
            assertThat(savedUser.getCustomInterest()).isEqualTo("임베디드 시스템");
        }

        @Test
        @DisplayName("OTHER 단독 선택 + customInterest 유효 → 성공 [SINT-034]")
        void signup_WithOnlyOtherInterestAndCustom_Succeeds() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    List.of(Interest.OTHER), "로보틱스",
                    JoinRoute.EVERYTIME, null,
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );

            // when
            PasswordSignupResponse response = signupService.signup(request);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("OTHER 미포함 + customInterest 있음 → 성공 (무시) [SINT-035]")
        void signup_WithoutOtherAndWithCustomInterest_Succeeds() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    List.of(Interest.WEB_FRONTEND), "임의값",
                    JoinRoute.EVERYTIME, null,
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );

            // when
            PasswordSignupResponse response = signupService.signup(request);

            // then
            assertThat(response).isNotNull();
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getCustomInterest()).isEqualTo("임의값");
        }
    }

    @Nested
    @DisplayName("회원가입 - OTHER 교차 검증 (가입 경로, INT-INV-04)")
    class SignupJoinRouteOtherValidationTest {

        @Test
        @DisplayName("OTHER + customJoinRoute null → 예외 [SINT-040]")
        void signup_WithOtherJoinRouteWithoutCustom_ThrowsException() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    List.of(Interest.WEB_FRONTEND), null,
                    JoinRoute.OTHER, null,
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(InvalidCustomFieldException.class);
        }

        @Test
        @DisplayName("OTHER + customJoinRoute 빈 문자열 → 예외 [SINT-041]")
        void signup_WithOtherJoinRouteWithBlankCustom_ThrowsException() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    List.of(Interest.WEB_FRONTEND), null,
                    JoinRoute.OTHER, "",
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(InvalidCustomFieldException.class);
        }

        @Test
        @DisplayName("OTHER + customJoinRoute 공백만 → 예외 [SINT-042]")
        void signup_WithOtherJoinRouteWithWhitespaceCustom_ThrowsException() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    List.of(Interest.WEB_FRONTEND), null,
                    JoinRoute.OTHER, "   ",
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(InvalidCustomFieldException.class);
        }

        @Test
        @DisplayName("OTHER + customJoinRoute 유효 → 성공 [SINT-043]")
        void signup_WithOtherJoinRouteAndCustom_Succeeds() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    List.of(Interest.WEB_FRONTEND), null,
                    JoinRoute.OTHER, "인스타그램 광고",
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );

            // when
            PasswordSignupResponse response = signupService.signup(request);

            // then
            assertThat(response).isNotNull();
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getJoinRoute()).isEqualTo(JoinRoute.OTHER);
            assertThat(savedUser.getCustomJoinRoute()).isEqualTo("인스타그램 광고");
        }

        @Test
        @DisplayName("OTHER 아닌 경우 + customJoinRoute 있음 → 성공 (무시) [SINT-044]")
        void signup_WithoutOtherAndWithCustomJoinRoute_Succeeds() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    List.of(Interest.WEB_FRONTEND), null,
                    JoinRoute.EVERYTIME, "임의값",
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );

            // when
            PasswordSignupResponse response = signupService.signup(request);

            // then
            assertThat(response).isNotNull();
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getCustomJoinRoute()).isEqualTo("임의값");
        }
    }

    @Nested
    @DisplayName("회원가입 - interests/joinRoute 정상 입력 및 저장")
    class SignupInterestsAndJoinRouteTest {

        @Test
        @DisplayName("interests/joinRoute 정상 입력 시 회원가입 성공 및 DB 저장 확인")
        void signup_WithValidInterestsAndJoinRoute_Succeeds() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    List.of(Interest.WEB_FRONTEND, Interest.AI, Interest.CLOUD), null,
                    JoinRoute.EVERYTIME, null,
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );

            // when
            signupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getInterests()).containsExactlyInAnyOrder(
                    Interest.WEB_FRONTEND, Interest.AI, Interest.CLOUD);
            assertThat(savedUser.getJoinRoute()).isEqualTo(JoinRoute.EVERYTIME);
        }
    }
}
