package igrus.web.security.auth.password.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.exception.account.AccountSuspendedException;
import igrus.web.security.auth.common.exception.account.AccountWithdrawnException;
import igrus.web.security.auth.common.exception.email.EmailNotVerifiedException;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.internal.LoginResult;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordAuthService 로그인/로그아웃 통합 테스트")
class PasswordAuthServiceLoginTest extends ServiceIntegrationTestBase {

    @Autowired
    private PasswordAuthService passwordAuthService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final long ACCESS_TOKEN_VALIDITY = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7일
    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_IP_ADDRESS = "192.168.1.100";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(passwordAuthService, "accessTokenValidity", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(passwordAuthService, "refreshTokenValidity", REFRESH_TOKEN_VALIDITY);
    }

    private User createAndSaveTestUser(UserRole role, UserStatus status) {
        User user = User.create(
                TEST_STUDENT_ID,
                "홍길동",
                "test@inha.edu",
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기"
        );
        user.changeRole(role);
        if (status == UserStatus.ACTIVE) {
            user.verifyEmail(); // PENDING_VERIFICATION -> ACTIVE
        } else if (status == UserStatus.SUSPENDED) {
            user.verifyEmail(); // 먼저 ACTIVE로 변경 후
            user.suspend();
        } else if (status == UserStatus.WITHDRAWN) {
            user.verifyEmail(); // 먼저 ACTIVE로 변경 후
            user.withdraw();
        }
        // PENDING_VERIFICATION은 기본 상태이므로 별도 처리 불필요
        return userRepository.save(user);
    }

    private PasswordCredential createAndSaveCredential(User user) {
        return createAndSaveCredential(user, UserStatus.ACTIVE);
    }

    private PasswordCredential createAndSaveCredential(User user, UserStatus status) {
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        PasswordCredential credential = PasswordCredential.create(user, encodedPassword);
        if (status == UserStatus.ACTIVE) {
            credential.verifyEmail(); // PENDING_VERIFICATION -> ACTIVE
        } else if (status == UserStatus.SUSPENDED) {
            credential.verifyEmail();
            credential.suspend();
        } else if (status == UserStatus.WITHDRAWN) {
            credential.verifyEmail();
            credential.withdraw();
        }
        // PENDING_VERIFICATION은 기본 상태이므로 별도 처리 불필요
        return passwordCredentialRepository.save(credential);
    }

    @Nested
    @DisplayName("로그인 성공 테스트")
    class LoginSuccessTest {

        @Test
        @DisplayName("준회원 로그인 성공 - Access Token과 Refresh Token 발급 [LOG-001]")
        void login_withAssociateRole_success() {
            // given
            User user = createAndSaveTestUser(UserRole.ASSOCIATE, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when
            LoginResult response = passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.refreshToken()).isNotNull();
            assertThat(response.role()).isEqualTo(UserRole.ASSOCIATE);
            assertThat(response.userId()).isEqualTo(user.getId());
            assertThat(response.studentId()).isEqualTo(TEST_STUDENT_ID);

            // RefreshToken이 DB에 저장되었는지 확인
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(response.refreshToken())).isPresent();
        }

        @Test
        @DisplayName("정회원 로그인 성공 - 역할 정보 MEMBER 반환 [LOG-002]")
        void login_withMemberRole_returnsMemberRole() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when
            LoginResult response = passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then
            assertThat(response.role()).isEqualTo(UserRole.MEMBER);
        }

        @Test
        @DisplayName("운영진 로그인 성공 - 역할 정보 OPERATOR 반환 [LOG-003]")
        void login_withOperatorRole_returnsOperatorRole() {
            // given
            User user = createAndSaveTestUser(UserRole.OPERATOR, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when
            LoginResult response = passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then
            assertThat(response.role()).isEqualTo(UserRole.OPERATOR);
        }

        @Test
        @DisplayName("관리자 로그인 성공 - 역할 정보 ADMIN 반환 [LOG-004]")
        void login_withAdminRole_returnsAdminRole() {
            // given
            User user = createAndSaveTestUser(UserRole.ADMIN, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when
            LoginResult response = passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then
            assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("Access Token 1시간 유효 - expiresIn 값 확인 [LOG-005]")
        void login_accessTokenValidity_isOneHour() {
            // given
            User user = createAndSaveTestUser(UserRole.ASSOCIATE, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when
            LoginResult response = passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then
            assertThat(response.accessTokenValidity()).isEqualTo(ACCESS_TOKEN_VALIDITY);
        }

        @Test
        @DisplayName("로그인 시 사용자 이름 반환 [LOG-007]")
        void login_returnsUserName() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when
            LoginResult response = passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then
            assertThat(response.name()).isEqualTo("홍길동");
        }
    }

    @Nested
    @DisplayName("로그인 실패 테스트")
    class LoginFailureTest {

        @Test
        @DisplayName("잘못된 학번으로 로그인 시도 - InvalidCredentialsException 발생 [LOG-010]")
        void login_withInvalidStudentId_throwsInvalidCredentialsException() {
            // given
            PasswordLoginRequest request = new PasswordLoginRequest("99999999", TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시도 - InvalidCredentialsException 발생 [LOG-011]")
        void login_withInvalidPassword_throwsInvalidCredentialsException() {
            // given
            User user = createAndSaveTestUser(UserRole.ASSOCIATE, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, "wrongPassword");

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("이메일 미인증 사용자 로그인 시도 - EmailNotVerifiedException 발생 [LOG-012]")
        void login_withUnverifiedEmail_throwsEmailNotVerifiedException() {
            // given - User 상태가 PENDING_VERIFICATION인 경우 (이메일 미인증)
            User user = createAndSaveTestUser(UserRole.ASSOCIATE, UserStatus.PENDING_VERIFICATION);
            createAndSaveCredential(user, UserStatus.PENDING_VERIFICATION);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(EmailNotVerifiedException.class);
        }

        @Test
        @DisplayName("비밀번호 정보가 없는 사용자 로그인 시도 - InvalidCredentialsException 발생 [LOG-013 관련]")
        void login_withNoPasswordCredential_throwsInvalidCredentialsException() {
            // given
            User user = createAndSaveTestUser(UserRole.ASSOCIATE, UserStatus.ACTIVE);
            // PasswordCredential을 저장하지 않음

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("계정 상태별 로그인 제한 테스트")
    class AccountStatusLoginTest {

        @Test
        @DisplayName("정지된 계정 로그인 시도 - AccountSuspendedException 발생 [LOG-020]")
        void login_withSuspendedAccount_throwsAccountSuspendedException() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.SUSPENDED);
            createAndSaveCredential(user, UserStatus.SUSPENDED);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(AccountSuspendedException.class);
        }

        @Test
        @DisplayName("탈퇴한 계정 로그인 시도 - AccountWithdrawnException 발생 [LOG-021]")
        void login_withWithdrawnAccount_throwsAccountWithdrawnException() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.WITHDRAWN);
            createAndSaveCredential(user, UserStatus.WITHDRAWN);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(AccountWithdrawnException.class);
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 요청 시 토큰 무효화 [LOG-030]")
        void logout_revokesRefreshToken() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            // 먼저 로그인하여 토큰 획득
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            LoginResult loginResponse = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);

            String refreshTokenString = loginResponse.refreshToken();

            // when
            passwordAuthService.logout(refreshTokenString);

            // then - 토큰이 무효화되었는지 확인
            Optional<RefreshToken> revokedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenString);
            assertThat(revokedToken).isEmpty();
        }

        @Test
        @DisplayName("로그아웃 후 이전 토큰 사용 불가 [LOG-031]")
        void logout_previousTokenInvalid() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            // 먼저 로그인하여 토큰 획득
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            LoginResult loginResponse = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);
            String refreshTokenString = loginResponse.refreshToken();

            // 로그아웃
            passwordAuthService.logout(refreshTokenString);

            // when & then - 로그아웃된 토큰으로 다시 로그아웃 시도
            assertThatThrownBy(() -> passwordAuthService.logout(refreshTokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("잘못된 토큰으로 로그아웃 시도 시 예외 발생 [LOG-032]")
        void logout_withInvalidRefreshToken_throwsException() {
            // given
            String invalidRefreshToken = "invalid.refresh.token";

            // when & then
            assertThatThrownBy(() -> passwordAuthService.logout(invalidRefreshToken))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }
    }

    @Nested
    @DisplayName("다중 디바이스 로그인 테스트")
    class MultiDeviceLoginTest {

        @Test
        @DisplayName("여러 기기 동시 로그인 - 각각 독립된 토큰 발급 [LOG-040]")
        void login_multipleDevices_issuesSeparateTokens() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when - 두 번 로그인
            LoginResult responseA = passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT);
            LoginResult responseB = passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then - 서로 다른 토큰이 발급됨
            assertThat(responseA.accessToken()).isNotEqualTo(responseB.accessToken());
            assertThat(responseA.refreshToken()).isNotEqualTo(responseB.refreshToken());

            // 둘 다 유효한 토큰
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(responseA.refreshToken())).isPresent();
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(responseB.refreshToken())).isPresent();
        }

        @Test
        @DisplayName("한 기기 로그아웃 시 다른 기기 유지 [LOG-041]")
        void logout_oneDevice_otherDeviceRemainsValid() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // 두 기기에서 로그인
            LoginResult responseA = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);
            LoginResult responseB = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // Device A 로그아웃
            passwordAuthService.logout(responseA.refreshToken());

            // then
            // Device A 토큰은 무효화됨
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(responseA.refreshToken())).isEmpty();
            // Device B 토큰은 여전히 유효함
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(responseB.refreshToken())).isPresent();
        }
    }
}
