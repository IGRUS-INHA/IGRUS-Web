package igrus.web.security.auth.password.service.auth;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.internal.LoginResult;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
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

@DisplayName("LogoutService 로그아웃 통합 테스트")
class LogoutServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private LoginService loginService;

    @Autowired
    private LogoutService logoutService;

    private static final long ACCESS_TOKEN_VALIDITY = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7일
    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_IP_ADDRESS = "192.168.1.100";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(loginService, "accessTokenValidity", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(loginService, "refreshTokenValidity", REFRESH_TOKEN_VALIDITY);
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
            LoginResult loginResponse = loginService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);

            String refreshTokenString = loginResponse.refreshToken();

            // when
            logoutService.logout(refreshTokenString);

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
            LoginResult loginResponse = loginService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);
            String refreshTokenString = loginResponse.refreshToken();

            // 로그아웃
            logoutService.logout(refreshTokenString);

            // when & then - 로그아웃된 토큰으로 다시 로그아웃 시도
            assertThatThrownBy(() -> logoutService.logout(refreshTokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("잘못된 토큰으로 로그아웃 시도 시 예외 발생 [LOG-032]")
        void logout_withInvalidRefreshToken_throwsException() {
            // given
            String invalidRefreshToken = "invalid.refresh.token";

            // when & then
            assertThatThrownBy(() -> logoutService.logout(invalidRefreshToken))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }
    }

    @Nested
    @DisplayName("다중 디바이스 로그아웃 테스트")
    class MultiDeviceLogoutTest {

        @Test
        @DisplayName("한 기기 로그아웃 시 다른 기기 유지 [LOG-041]")
        void logout_oneDevice_otherDeviceRemainsValid() {
            // given
            User user = createAndSaveTestUser(UserRole.MEMBER, UserStatus.ACTIVE);
            createAndSaveCredential(user, UserStatus.ACTIVE);

            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // 두 기기에서 로그인
            LoginResult responseA = loginService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);
            LoginResult responseB = loginService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // Device A 로그아웃
            logoutService.logout(responseA.refreshToken());

            // then
            // Device A 토큰은 무효화됨
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(responseA.refreshToken())).isEmpty();
            // Device B 토큰은 여전히 유효함
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(responseB.refreshToken())).isPresent();
        }
    }
}
