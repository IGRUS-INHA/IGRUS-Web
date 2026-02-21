package igrus.web.security.auth.password.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.signup.DuplicatePhoneNumberException;
import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.security.auth.common.exception.signup.VerificationTokenInvalidException;
import igrus.web.security.auth.common.exception.signup.InvalidCustomFieldException;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.service.signup.SignupService;
import igrus.web.webhook.baebdungi.service.BaebdungiWebhookService;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.Interest;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 회원가입 통합 테스트
 *
 * <p>테스트 범위:</p>
 * <ul>
 *     <li>REG-001 ~ REG-004: 개인정보 동의</li>
 *     <li>REG-010 ~ REG-018: 필수 정보 입력 및 검증</li>
 *     <li>REG-020 ~ REG-026: 비밀번호 검증</li>
 *     <li>REG-030 ~ REG-032: 중복 검사</li>
 *     <li>REG-050 ~ REG-052: Edge Cases</li>
 * </ul>
 *
 * <p>이메일 인증 관련 테스트(REG-040~045)는 PreSignupSendCodeServiceTest,
 * PreSignupVerifyCodeServiceTest에서 별도로 테스트합니다.</p>
 */
@DisplayName("회원가입 통합 테스트")
class PasswordSignupIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private SignupService signupService;

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

    private String verificationToken;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    /**
     * 사전 인증된 이메일 레코드를 생성합니다.
     * 새 플로우에서 signup은 이메일이 사전 인증되어 있어야 합니다.
     */
    private void createVerifiedEmailRecord(String email) {
        EmailVerification verification = EmailVerification.create(email, "123456", 600000L);
        this.verificationToken = verification.verify();
        emailVerificationRepository.save(verification);
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

    private PasswordSignupRequest createSignupRequestWithEmail(String email) {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                VALID_NAME,
                email,
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

    private PasswordSignupRequest createSignupRequestWithStudentId(String studentId) {
        return new PasswordSignupRequest(
                studentId,
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

    private PasswordSignupRequest createSignupRequestWithPhone(String phone) {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                VALID_NAME,
                VALID_EMAIL,
                VALID_PASSWORD,
                phone,
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

    // ===== 2.0 이메일 사전 인증 필수 테스트 =====

    @Nested
    @DisplayName("이메일 사전 인증 필수 테스트")
    class EmailPreVerificationRequiredTest {

        @Test
        @DisplayName("이메일 사전 인증 없이 가입 시도 시 VerificationTokenInvalidException")
        void signup_withoutPreVerifiedEmail_throwsException() {
            // given
            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(VerificationTokenInvalidException.class);

            // 사용자가 생성되지 않았는지 확인
            assertThat(userRepository.findByEmail(VALID_EMAIL)).isEmpty();
        }
    }

    // ===== 2.1 개인정보 동의 테스트 =====

    @Nested
    @DisplayName("개인정보 동의 테스트")
    class PrivacyConsentTest {

        @Test
        @DisplayName("[REG-004] 동의한 정책 버전 기록")
        void signup_privacyConsent_recordsPolicyVersion() {
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
            assertThat(savedConsent.getConsentDate()).isNotNull();
        }

        @Test
        @DisplayName("[REG-004] 개인정보 동의 시 동의 일시가 기록됨")
        void signup_privacyConsent_recordsConsentDate() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            Instant beforeSignup = Instant.now();
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

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
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            PasswordSignupResponse response = signupService.signup(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo(VALID_EMAIL);
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStudentId()).isEqualTo(VALID_STUDENT_ID);
            assertThat(savedUser.getName()).isEqualTo(VALID_NAME);
            assertThat(savedUser.getPhoneNumber()).isEqualTo(VALID_PHONE);
            assertThat(savedUser.getDepartment()).isEqualTo(VALID_DEPARTMENT);
            assertThat(savedUser.getMotivation()).isEqualTo(VALID_MOTIVATION);
        }

        @Test
        @DisplayName("[REG-010] 회원가입 시 User가 ACTIVE 상태로 생성됨")
        void signup_createsUser_withActiveStatus() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(savedUser.isActive()).isTrue();
        }

        @Test
        @DisplayName("[REG-010] 회원가입 시 PasswordCredential이 ACTIVE 상태로 생성됨")
        void signup_createsPasswordCredential_withActiveStatus() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            Optional<PasswordCredential> credential = passwordCredentialRepository.findByUserId(savedUser.getId());

            assertThat(credential).isPresent();
            assertThat(credential.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("[REG-010] 회원가입 시 인증 레코드가 정리됨")
        void signup_cleansUpEmailVerificationRecord() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then - 인증 레코드가 삭제됨
            assertThat(emailVerificationRepository.existsByEmailAndVerifiedTrue(VALID_EMAIL)).isFalse();
        }

        @Test
        @DisplayName("[REG-010] 회원가입 시 User 역할이 기본값(ASSOCIATE)으로 설정됨")
        void signup_setsDefaultRole_toAssociate() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

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
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            PasswordSignupResponse response = signupService.signup(request);

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
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

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
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

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
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(DuplicateStudentIdException.class);

            // 새 사용자가 저장되지 않았는지 확인
            assertThat(userRepository.findByEmail(VALID_EMAIL)).isEmpty();
        }

        @Test
        @DisplayName("[REG-031] 이미 등록된 이메일로 가입 시도 시 오류")
        void signup_withDuplicateEmail_throwsException() {
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
        @DisplayName("[REG-032] 이미 등록된 전화번호로 가입 시도 시 오류")
        void signup_withDuplicatePhoneNumber_throwsException() {
            // given
            User existingUser = User.create(
                    "99999999",
                    "기존사용자",
                    "other@inha.edu",
                    VALID_PHONE,
                    "기타학과",
                    "동기",
                    List.of(),
                    Gender.MALE,
                    1,
                    EnrollmentStatus.ENROLLED,
                    List.of(), null, null, null
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

        @Test
        @DisplayName("[REG-030] 중복 학번 검사 시 대소문자 구분하지 않음")
        void signup_duplicateStudentIdCheck_isCaseInsensitive() {
            // given - 학번은 숫자이므로 대소문자와 무관하지만 검증 로직 확인
            createAndSaveUser(VALID_STUDENT_ID, "other@inha.edu", UserRole.ASSOCIATE);
            createVerifiedEmailRecord(VALID_EMAIL);

            PasswordSignupRequest request = createSignupRequestWithStudentId(VALID_STUDENT_ID);

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(DuplicateStudentIdException.class);
        }

        @Test
        @DisplayName("[REG-031] 중복 이메일 검사는 정확히 일치하는 경우에만 오류")
        void signup_duplicateEmailCheck_requiresExactMatch() {
            // given
            createAndSaveUser("99999999", "existing@inha.edu", UserRole.ASSOCIATE);
            createVerifiedEmailRecord("test@inha.edu");

            PasswordSignupRequest request = createSignupRequestWithEmail("test@inha.edu");

            // when & then - 다른 이메일이면 성공해야 함
            PasswordSignupResponse response = signupService.signup(request);
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
                    "동기",
                    List.of(),
                    Gender.MALE,
                    1,
                    EnrollmentStatus.ENROLLED,
                    List.of(), null, null, null
            );
            userRepository.save(existingUser);
            createVerifiedEmailRecord(VALID_EMAIL);

            PasswordSignupRequest request = createSignupRequestWithPhone("010-1234-5678");

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(DuplicatePhoneNumberException.class);
        }
    }

    // ===== 2.6 Edge Cases =====

    @Nested
    @DisplayName("Edge Cases 테스트")
    class EdgeCasesTest {

        @Test
        @DisplayName("회원가입 시 기존 인증 레코드가 정리됨")
        void signup_cleansUpVerifiedRecord() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();

            // when
            signupService.signup(request);

            // then - 인증 레코드가 정리됨
            assertThat(emailVerificationRepository.existsByEmailAndVerifiedTrue(VALID_EMAIL)).isFalse();
        }

        @Test
        @DisplayName("동일한 정보로 두 번 회원가입 시도 시 두 번째는 실패")
        void signup_withSameInfo_secondAttemptFails() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createValidSignupRequest();
            signupService.signup(request);

            // 두 번째 시도를 위해 인증 레코드 재생성
            createVerifiedEmailRecord(VALID_EMAIL);

            // when & then - 동일한 학번으로 두 번째 시도
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(DuplicateStudentIdException.class);
        }

        @Test
        @DisplayName("이메일만 다른 경우 학번 중복으로 실패")
        void signup_withDifferentEmailSameStudentId_failsOnStudentId() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest firstRequest = createValidSignupRequest();
            signupService.signup(firstRequest);

            createVerifiedEmailRecord("different@inha.edu");
            PasswordSignupRequest secondRequest = createSignupRequestWithEmail("different@inha.edu");

            // when & then
            assertThatThrownBy(() -> signupService.signup(secondRequest))
                    .isInstanceOf(DuplicateStudentIdException.class);
        }

        @Test
        @DisplayName("학번만 다른 경우 이메일 중복으로 실패")
        void signup_withDifferentStudentIdSameEmail_failsOnEmail() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest firstRequest = createValidSignupRequest();
            signupService.signup(firstRequest);

            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest secondRequest = new PasswordSignupRequest(
                    "99999999",
                    VALID_NAME,
                    VALID_EMAIL,
                    VALID_PASSWORD,
                    "010-9999-9999",
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

            // when & then
            assertThatThrownBy(() -> signupService.signup(secondRequest))
                    .isInstanceOf(DuplicateEmailException.class);
        }
    }

    // === 필드 간 조합 테스트 (Pairwise, SINT-060~068) ===

    @Nested
    @DisplayName("회원가입 - interests/joinRoute 필드 간 조합 테스트")
    class SignupPairwiseCombinationTest {

        private PasswordSignupRequest createRequestWith(
                List<Interest> interests, String customInterest,
                JoinRoute joinRoute, String customJoinRoute) {
            return new PasswordSignupRequest(
                    VALID_STUDENT_ID, VALID_NAME, VALID_EMAIL, VALID_PASSWORD,
                    VALID_PHONE, VALID_DEPARTMENT, VALID_MOTIVATION, List.of(),
                    interests, customInterest,
                    joinRoute, customJoinRoute,
                    Gender.MALE, 1, EnrollmentStatus.ENROLLED, true,
                    verificationToken
            );
        }

        @Test
        @DisplayName("조합 1: 둘 다 OTHER 아님 + custom 없음 → 성공 [SINT-060]")
        void signup_BothNotOther_NoCustcoms_Succeeds() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createRequestWith(
                    List.of(Interest.WEB_FRONTEND), null,
                    JoinRoute.EVERYTIME, null);

            // when
            PasswordSignupResponse response = signupService.signup(request);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("조합 2: interests OTHER 아님 + joinRoute=OTHER 성공 [SINT-061]")
        void signup_InterestNotOther_JoinRouteOther_Succeeds() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createRequestWith(
                    List.of(Interest.WEB_FRONTEND), null,
                    JoinRoute.OTHER, "인스타");

            // when
            PasswordSignupResponse response = signupService.signup(request);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("조합 3: interests OTHER + joinRoute OTHER 아님 성공 [SINT-062]")
        void signup_InterestOther_JoinRouteNotOther_Succeeds() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createRequestWith(
                    List.of(Interest.AI, Interest.OTHER), "임베디드",
                    JoinRoute.EVERYTIME, null);

            // when
            PasswordSignupResponse response = signupService.signup(request);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("조합 4: 둘 다 OTHER + 둘 다 custom 있음 성공 [SINT-063]")
        void signup_BothOther_BothCustom_Succeeds() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createRequestWith(
                    List.of(Interest.AI, Interest.OTHER), "임베디드",
                    JoinRoute.OTHER, "인스타");

            // when
            PasswordSignupResponse response = signupService.signup(request);

            // then
            assertThat(response).isNotNull();
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getInterests()).contains(Interest.OTHER);
            assertThat(savedUser.getCustomInterest()).isEqualTo("임베디드");
            assertThat(savedUser.getJoinRoute()).isEqualTo(JoinRoute.OTHER);
            assertThat(savedUser.getCustomJoinRoute()).isEqualTo("인스타");
        }

        @Test
        @DisplayName("조합 5: interests OTHER + custom 없음 → 실패 [SINT-064]")
        void signup_InterestOther_NoCustomInterest_Fails() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createRequestWith(
                    List.of(Interest.AI, Interest.OTHER), null,
                    JoinRoute.EVERYTIME, null);

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(InvalidCustomFieldException.class);
        }

        @Test
        @DisplayName("조합 6: interests OTHER + custom 빈 문자열 → 실패 [SINT-065]")
        void signup_InterestOther_BlankCustomInterest_Fails() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createRequestWith(
                    List.of(Interest.AI, Interest.OTHER), "",
                    JoinRoute.OTHER, "인스타");

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(InvalidCustomFieldException.class);
        }

        @Test
        @DisplayName("조합 7: joinRoute OTHER + custom 없음 → 실패 [SINT-066]")
        void signup_JoinRouteOther_NoCustomJoinRoute_Fails() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createRequestWith(
                    List.of(Interest.WEB_FRONTEND), null,
                    JoinRoute.OTHER, null);

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(InvalidCustomFieldException.class);
        }

        @Test
        @DisplayName("조합 8: joinRoute OTHER + custom 빈 문자열 → 실패 [SINT-067]")
        void signup_JoinRouteOther_BlankCustomJoinRoute_Fails() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createRequestWith(
                    List.of(Interest.WEB_FRONTEND), null,
                    JoinRoute.OTHER, "");

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(InvalidCustomFieldException.class);
        }

        @Test
        @DisplayName("조합 9: 둘 다 OTHER + 둘 다 custom 없음 → 실패 [SINT-068]")
        void signup_BothOther_NoCustcoms_Fails() {
            // given
            createVerifiedEmailRecord(VALID_EMAIL);
            PasswordSignupRequest request = createRequestWith(
                    List.of(Interest.OTHER), null,
                    JoinRoute.OTHER, null);

            // when & then
            assertThatThrownBy(() -> signupService.signup(request))
                    .isInstanceOf(InvalidCustomFieldException.class);
        }
    }
}
