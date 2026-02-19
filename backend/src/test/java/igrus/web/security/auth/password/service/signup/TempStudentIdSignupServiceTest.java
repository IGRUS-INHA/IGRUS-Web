package igrus.web.security.auth.password.service.signup;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.signup.DuplicatePhoneNumberException;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.dto.request.TemporaryStudentIdSignupRequest;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.user.domain.*;
import igrus.web.user.exception.TempStudentIdNotAvailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TempStudentIdSignupService 통합 테스트")
class TempStudentIdSignupServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private TempStudentIdSignupService tempStudentIdSignupService;

    @MockitoBean
    private AuthEmailService authEmailService;

    @MockitoBean
    private Clock clock;

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final String VALID_NAME = "신입생";
    private static final String VALID_EMAIL = "newbie@inha.edu";
    private static final String VALID_PASSWORD = "testpass1";
    private static final String VALID_PHONE = "010-9876-5432";
    private static final String VALID_DEPARTMENT = "컴퓨터공학과";

    @BeforeEach
    void setUp() {
        setUpBase();
        setClock(2026, 1, 15); // 1월로 설정
        ReflectionTestUtils.setField(tempStudentIdSignupService, "verificationCodeExpiry", 600000L);
    }

    private void setClock(int year, int month, int day) {
        Instant fixedInstant = java.time.LocalDate.of(year, month, day)
                .atStartOfDay(KOREA_ZONE).toInstant();
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(KOREA_ZONE);
    }

    private TemporaryStudentIdSignupRequest createValidRequest() {
        return new TemporaryStudentIdSignupRequest(
                VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_PHONE,
                VALID_DEPARTMENT, "프로그래밍을 배우고 싶습니다.",
                List.of(), List.of(Interest.WEB_FRONTEND), null,
                JoinRoute.EVERYTIME, null, Gender.MALE, 1,
                EnrollmentStatus.ENROLLED, true
        );
    }

    @Nested
    @DisplayName("임시 학번 회원가입 - 정상 케이스")
    class SignupSuccessTest {

        @Test
        @DisplayName("1월, 1학년 정상 가입 시 임시 학번 발급 [TEMP-INV-01~05, 09]")
        void signup_ValidRequest_CreatesUserWithTempId() {
            // given
            TemporaryStudentIdSignupRequest request = createValidRequest();

            // when
            PasswordSignupResponse response = tempStudentIdSignupService.signup(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo(VALID_EMAIL);
            assertThat(response.requiresVerification()).isTrue();

            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.getStudentId()).startsWith("9926");
            assertThat(savedUser.getStudentId()).hasSize(8);
        }

        @Test
        @DisplayName("응답에 임시 학번 포함 [TEMP-INV-01]")
        void signup_ValidRequest_ResponseContainsTempId() {
            // when
            PasswordSignupResponse response = tempStudentIdSignupService.signup(createValidRequest());

            // then
            assertThat(response.temporaryStudentId()).isNotNull();
            assertThat(response.temporaryStudentId()).startsWith("9926");
            assertThat(response.temporaryStudentId()).hasSize(8);
        }

        @Test
        @DisplayName("hasTemporaryStudentId 플래그 true 설정 확인 [TEMP-INV-05]")
        void signup_ValidRequest_SetsTemporaryFlag() {
            // when
            tempStudentIdSignupService.signup(createValidRequest());

            // then
            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElseThrow();
            assertThat(savedUser.isHasTemporaryStudentId()).isTrue();
        }

        @Test
        @DisplayName("인증 이메일 + 임시 학번 이메일 발송 확인 [TEMP-INV-09]")
        void signup_ValidRequest_SendsTempIdEmail() {
            // when
            tempStudentIdSignupService.signup(createValidRequest());

            // then
            verify(authEmailService).sendVerificationEmail(eq(VALID_EMAIL), anyString());
            verify(authEmailService).sendTemporaryStudentIdEmail(eq(VALID_EMAIL), eq(VALID_NAME), anyString());
        }

        @Test
        @DisplayName("2월 가입도 정상 처리")
        void signup_InFebruary_Succeeds() {
            // given
            setClock(2026, 2, 28);

            // when
            PasswordSignupResponse response = tempStudentIdSignupService.signup(createValidRequest());

            // then
            assertThat(response.temporaryStudentId()).startsWith("9926");
        }
    }

    @Nested
    @DisplayName("임시 학번 회원가입 - 기간 제한")
    class SignupPeriodTest {

        @Test
        @DisplayName("3월 요청 시 TempStudentIdNotAvailableException [TEMP-INV-03]")
        void signup_InMarch_ThrowsTempIdNotAvailable() {
            // given
            setClock(2026, 3, 1);

            // when & then
            assertThatThrownBy(() -> tempStudentIdSignupService.signup(createValidRequest()))
                    .isInstanceOf(TempStudentIdNotAvailableException.class);
        }

        @Test
        @DisplayName("12월 요청 시 TempStudentIdNotAvailableException [TEMP-INV-03]")
        void signup_InDecember_ThrowsTempIdNotAvailable() {
            // given
            setClock(2026, 12, 25);

            // when & then
            assertThatThrownBy(() -> tempStudentIdSignupService.signup(createValidRequest()))
                    .isInstanceOf(TempStudentIdNotAvailableException.class);
        }
    }

    @Nested
    @DisplayName("임시 학번 회원가입 - 중복 검증")
    class SignupDuplicateTest {

        @Test
        @DisplayName("이메일 중복 시 DuplicateEmailException")
        void signup_DuplicateEmail_ThrowsDuplicateException() {
            // given
            createAndSaveUser("20231111", VALID_EMAIL, UserRole.ASSOCIATE);

            // when & then
            assertThatThrownBy(() -> tempStudentIdSignupService.signup(createValidRequest()))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        @DisplayName("전화번호 중복 시 DuplicatePhoneNumberException")
        void signup_DuplicatePhone_ThrowsDuplicateException() {
            // given
            User existing = User.create(
                    "20231111", "기존유저", "other@inha.edu", VALID_PHONE,
                    "기타학과", "동기", List.of(), Gender.MALE, 1,
                    EnrollmentStatus.ENROLLED, List.of(), null, null, null
            );
            userRepository.save(existing);

            // when & then
            assertThatThrownBy(() -> tempStudentIdSignupService.signup(createValidRequest()))
                    .isInstanceOf(DuplicatePhoneNumberException.class);
        }
    }
}
