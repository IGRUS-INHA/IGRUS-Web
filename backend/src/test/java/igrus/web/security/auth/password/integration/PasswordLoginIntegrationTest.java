package igrus.web.security.auth.password.integration;

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
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.Gender;
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

/**
 * 로그인 통합 테스트 (21개 테스트 케이스)
 *
 * <p>테스트 케이스 문서: docs/test-case/auth/login-test-cases.md</p>
 *
 * <p>테스트 범위:</p>
 * <ul>
 *     <li>LOG-001 ~ LOG-007: 로그인 성공</li>
 *     <li>LOG-010 ~ LOG-014: 로그인 실패</li>
 *     <li>LOG-020 ~ LOG-022: 계정 상태별 로그인 제한</li>
 *     <li>LOG-030 ~ LOG-032: 로그아웃</li>
 *     <li>LOG-040 ~ LOG-041: 다중 디바이스 로그인</li>
 * </ul>
 */
@DisplayName("로그인 통합 테스트")
class PasswordLoginIntegrationTest extends ServiceIntegrationTestBase {

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
                "테스트 동기",
                Gender.MALE,
                1
        );
        user.changeRole(role);
        if (status == UserStatus.ACTIVE) {
            user.verifyEmail();
        } else if (status == UserStatus.SUSPENDED) {
            user.verifyEmail();
            user.suspend();
        } else if (status == UserStatus.WITHDRAWN) {
            user.verifyEmail();
            user.withdraw();
        }
        return userRepository.save(user);
    }

    private PasswordCredential createAndSaveCredential(User user, UserStatus status) {
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        PasswordCredential credential = PasswordCredential.create(user, encodedPassword);
        if (status == UserStatus.ACTIVE) {
            credential.verifyEmail();
        } else if (status == UserStatus.SUSPENDED) {
            credential.verifyEmail();
            credential.suspend();
        } else if (status == UserStatus.WITHDRAWN) {
            credential.verifyEmail();
            credential.withdraw();
        }
        return passwordCredentialRepository.save(credential);
    }

    // ===== 2.1 로그인 성공 테스트 =====

    @Nested
    @DisplayName("로그인 성공 테스트")
    class LoginSuccessTest {

        @Test
        @DisplayName("[LOG-001] 준회원 로그인 성공 - Access Token과 Refresh Token 발급")
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
        @DisplayName("[LOG-002] 정회원 로그인 성공 - 역할 정보 MEMBER 반환")
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
        @DisplayName("[LOG-003] 운영진 로그인 성공 - 역할 정보 OPERATOR 반환")
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
        @DisplayName("[LOG-004] 관리자 로그인 성공 - 역할 정보 ADMIN 반환")
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
        @DisplayName("[LOG-005] Access Token 1시간 유효 - expiresIn 값 확인")
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
        @DisplayName("[LOG-006] Refresh Token 7일 유효 - DB 저장 확인")
        void login_refreshTokenValidity_isSevenDays() {
            // given
            User user = createAndSaveTestUser(UserRole.ASSOCIATE, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when
            LoginResult response = passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then
            RefreshToken savedToken = refreshTokenRepository.findByTokenAndRevokedFalse(response.refreshToken()).orElseThrow();
            assertThat(savedToken.isExpired()).isFalse();
        }

        @Test
        @DisplayName("[LOG-007] 로그인 시 사용자 이름 반환")
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

    // ===== 2.2 로그인 실패 테스트 =====

    @Nested
    @DisplayName("로그인 실패 테스트")
    class LoginFailureTest {

        @Test
        @DisplayName("[LOG-010] 잘못된 학번으로 로그인 시도 - InvalidCredentialsException 발생")
        void login_withInvalidStudentId_throwsInvalidCredentialsException() {
            // given
            PasswordLoginRequest request = new PasswordLoginRequest("99999999", TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("[LOG-011] 잘못된 비밀번호로 로그인 시도 - InvalidCredentialsException 발생")
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
        @DisplayName("[LOG-012] 이메일 미인증 사용자 로그인 시도 - EmailNotVerifiedException 발생")
        void login_withUnverifiedEmail_throwsEmailNotVerifiedException() {
            // given
            User user = createAndSaveTestUser(UserRole.ASSOCIATE, UserStatus.PENDING_VERIFICATION);
            createAndSaveCredential(user, UserStatus.PENDING_VERIFICATION);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(EmailNotVerifiedException.class);
        }

        @Test
        @DisplayName("[LOG-013] 비밀번호 정보가 없는 사용자 로그인 시도 - InvalidCredentialsException 발생")
        void login_withNoPasswordCredential_throwsInvalidCredentialsException() {
            // given
            User user = createAndSaveTestUser(UserRole.ASSOCIATE, UserStatus.ACTIVE);
            // PasswordCredential을 저장하지 않음

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("[LOG-014] 존재하지 않는 학번으로 로그인 시도 - InvalidCredentialsException 발생")
        void login_withNonExistentStudentId_throwsInvalidCredentialsException() {
            // given
            PasswordLoginRequest request = new PasswordLoginRequest("00000000", "anyPassword");

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    // ===== 2.3 계정 상태별 로그인 제한 테스트 =====

    @Nested
    @DisplayName("계정 상태별 로그인 제한 테스트")
    class AccountStatusLoginTest {

        @Test
        @DisplayName("[LOG-020] 정지된 계정 로그인 시도 - AccountSuspendedException 발생")
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
        @DisplayName("[LOG-021] 탈퇴한 계정 로그인 시도 - AccountWithdrawnException 발생")
        void login_withWithdrawnAccount_throwsAccountWithdrawnException() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.WITHDRAWN);
            createAndSaveCredential(user, UserStatus.WITHDRAWN);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(AccountWithdrawnException.class);
        }

        @Test
        @DisplayName("[LOG-022] 정지된 계정 비밀번호 틀렸을 때 - 비밀번호 오류 우선")
        void login_withSuspendedAccountAndWrongPassword_throwsInvalidCredentialsException() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.SUSPENDED);
            createAndSaveCredential(user, UserStatus.SUSPENDED);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, "wrongPassword");

            // when & then - 비밀번호 검증이 먼저 실패해야 함
            assertThatThrownBy(() -> passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    // ===== 2.4 로그아웃 테스트 =====

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {

        @Test
        @DisplayName("[LOG-030] 로그아웃 요청 시 토큰 무효화")
        void logout_revokesRefreshToken() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            LoginResult loginResponse = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);

            String refreshTokenString = loginResponse.refreshToken();

            // when
            passwordAuthService.logout(refreshTokenString);

            // then
            Optional<RefreshToken> revokedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenString);
            assertThat(revokedToken).isEmpty();
        }

        @Test
        @DisplayName("[LOG-031] 로그아웃 후 이전 Refresh Token 사용 불가")
        void logout_previousTokenInvalid() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            LoginResult loginResponse = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);
            String refreshTokenString = loginResponse.refreshToken();

            passwordAuthService.logout(refreshTokenString);

            // when & then - 로그아웃된 토큰으로 다시 로그아웃 시도
            assertThatThrownBy(() -> passwordAuthService.logout(refreshTokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("[LOG-032] 잘못된 토큰으로 로그아웃 시도 시 예외 발생")
        void logout_withInvalidRefreshToken_throwsException() {
            // given
            String invalidRefreshToken = "invalid.refresh.token";

            // when & then
            assertThatThrownBy(() -> passwordAuthService.logout(invalidRefreshToken))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }
    }

    // ===== 2.5 다중 디바이스 로그인 테스트 =====

    @Nested
    @DisplayName("다중 디바이스 로그인 테스트")
    class MultiDeviceLoginTest {

        @Test
        @DisplayName("[LOG-040] 여러 기기 동시 로그인 - 각각 독립된 토큰 발급")
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
        @DisplayName("[LOG-041] 한 기기 로그아웃 시 다른 기기 유지")
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

        @Test
        @DisplayName("[LOG-040] 동시 로그인 시 사용자 정보는 동일")
        void login_multipleDevices_sameUserInfo() {
            // given
            User user = createAndSaveTestUser(UserRole.OPERATOR, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when
            LoginResult responseA = passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT);
            LoginResult responseB = passwordAuthService.login(request, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then - 사용자 정보는 동일
            assertThat(responseA.userId()).isEqualTo(responseB.userId());
            assertThat(responseA.studentId()).isEqualTo(responseB.studentId());
            assertThat(responseA.name()).isEqualTo(responseB.name());
            assertThat(responseA.role()).isEqualTo(responseB.role());
        }
    }
}
