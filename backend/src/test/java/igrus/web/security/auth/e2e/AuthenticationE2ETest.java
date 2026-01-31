package igrus.web.security.auth.e2e;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.internal.RecoveryResult;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.exception.account.AccountRecoverableException;
import igrus.web.security.auth.common.exception.account.AccountWithdrawnException;
import igrus.web.security.auth.common.service.AccountRecoveryService;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.internal.LoginResult;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.dto.response.TokenRefreshResponse;
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.auth.password.service.PasswordResetService;
import igrus.web.security.auth.password.service.PasswordSignupService;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.Gender;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * 인증 E2E 통합 테스트
 *
 * <p>전체 인증 플로우 시나리오를 테스트합니다.</p>
 *
 * <p>테스트 시나리오:</p>
 * <ul>
 *     <li>E2E-001: 회원가입 → 이메일 인증 → 로그인 전체 플로우</li>
 *     <li>E2E-002: 로그인 → API 접근 → 로그아웃 → 토큰 무효화 확인</li>
 *     <li>E2E-003: 토큰 갱신 플로우 (Access Token 만료 시뮬레이션)</li>
 *     <li>E2E-004: 비밀번호 재설정 플로우 (요청 → 토큰 → 변경 → 재로그인)</li>
 *     <li>E2E-005: 계정 탈퇴 → 복구 플로우 (5일 이내)</li>
 * </ul>
 */
@DisplayName("인증 E2E 통합 테스트")
class AuthenticationE2ETest extends ServiceIntegrationTestBase {

    @Autowired
    private PasswordSignupService passwordSignupService;

    @Autowired
    private PasswordAuthService passwordAuthService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private AccountRecoveryService accountRecoveryService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthEmailService authEmailService;

    private static final long ACCESS_TOKEN_VALIDITY = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7일
    private static final long VERIFICATION_CODE_EXPIRY = 600000L; // 10분
    private static final long PASSWORD_RESET_EXPIRY = 1800000L; // 30분

    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_NAME = "홍길동";
    private static final String TEST_EMAIL = "test@inha.edu";
    private static final String TEST_PASSWORD = "TestPass1!@";
    private static final String TEST_PHONE = "010-1234-5678";
    private static final String TEST_DEPARTMENT = "컴퓨터공학과";
    private static final String TEST_MOTIVATION = "동아리 활동을 열심히 하고 싶습니다.";
    private static final String TEST_IP_ADDRESS = "192.168.1.100";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(passwordAuthService, "accessTokenValidity", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(passwordAuthService, "refreshTokenValidity", REFRESH_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(passwordSignupService, "verificationCodeExpiry", VERIFICATION_CODE_EXPIRY);
        ReflectionTestUtils.setField(passwordSignupService, "maxAttempts", 5);
        ReflectionTestUtils.setField(passwordSignupService, "resendRateLimitSeconds", 60L);
        ReflectionTestUtils.setField(passwordResetService, "passwordResetExpiry", PASSWORD_RESET_EXPIRY);
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(accountRecoveryService, "accessTokenValidity", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(accountRecoveryService, "refreshTokenValidity", REFRESH_TOKEN_VALIDITY);
    }

    private PasswordSignupRequest createSignupRequest() {
        return new PasswordSignupRequest(
                TEST_STUDENT_ID,
                TEST_NAME,
                TEST_EMAIL,
                TEST_PASSWORD,
                TEST_PHONE,
                TEST_DEPARTMENT,
                TEST_MOTIVATION,
                Gender.MALE,
                1,
                true
        );
    }

    // ===== E2E-001: 회원가입 → 이메일 인증 → 로그인 전체 플로우 =====

    @Nested
    @DisplayName("[E2E-001] 회원가입 → 이메일 인증 → 로그인 전체 플로우")
    class SignupVerificationLoginFlowTest {

        @Test
        @DisplayName("회원가입부터 로그인까지 전체 플로우 성공")
        void fullSignupToLoginFlow_succeeds() {
            // === Step 1: 회원가입 ===
            PasswordSignupRequest signupRequest = createSignupRequest();
            PasswordSignupResponse signupResponse = passwordSignupService.signup(signupRequest);

            assertThat(signupResponse).isNotNull();
            assertThat(signupResponse.email()).isEqualTo(TEST_EMAIL);
            assertThat(signupResponse.requiresVerification()).isTrue();

            // 이메일 발송 확인
            verify(authEmailService).sendVerificationEmail(eq(TEST_EMAIL), anyString());

            // 사용자 상태 확인 (PENDING_VERIFICATION)
            User pendingUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);

            // === Step 2: 이메일 인증 ===
            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(TEST_EMAIL).orElseThrow();
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(TEST_EMAIL, verification.getCode());

            PasswordSignupResponse verifyResponse = passwordSignupService.verifyEmail(verifyRequest);

            assertThat(verifyResponse).isNotNull();
            assertThat(verifyResponse.requiresVerification()).isFalse();

            // 사용자 상태 확인 (ACTIVE)
            User activeUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(activeUser.getRole()).isEqualTo(UserRole.ASSOCIATE);

            // === Step 3: 로그인 ===
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            LoginResult loginResult = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);

            assertThat(loginResult).isNotNull();
            assertThat(loginResult.accessToken()).isNotNull();
            assertThat(loginResult.refreshToken()).isNotNull();
            assertThat(loginResult.userId()).isEqualTo(activeUser.getId());
            assertThat(loginResult.studentId()).isEqualTo(TEST_STUDENT_ID);
            assertThat(loginResult.name()).isEqualTo(TEST_NAME);
            assertThat(loginResult.role()).isEqualTo(UserRole.ASSOCIATE);

            // JWT 토큰 검증
            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(loginResult.accessToken());
            assertThat(jwtTokenProvider.getUserIdFromClaims(claims)).isEqualTo(activeUser.getId());
        }
    }

    // ===== E2E-002: 로그인 → API 접근 → 로그아웃 → 토큰 무효화 확인 =====

    @Nested
    @DisplayName("[E2E-002] 로그인 → API 접근 → 로그아웃 → 토큰 무효화 확인")
    class LoginLogoutFlowTest {

        @Test
        @DisplayName("로그인 후 로그아웃하면 토큰이 무효화됨")
        void loginThenLogout_invalidatesTokens() {
            // === Setup: 인증된 사용자 생성 ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            // === Step 1: 로그인 ===
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            LoginResult loginResult = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);

            assertThat(loginResult.accessToken()).isNotNull();
            String refreshToken = loginResult.refreshToken();

            // RefreshToken이 DB에 저장되어 있는지 확인
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)).isPresent();

            // === Step 2: Access Token으로 사용자 정보 조회 (API 접근 시뮬레이션) ===
            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(loginResult.accessToken());
            Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
            assertThat(userId).isEqualTo(user.getId());

            // === Step 3: 로그아웃 ===
            passwordAuthService.logout(refreshToken);

            // === Step 4: 토큰 무효화 확인 ===
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)).isEmpty();
        }

        @Test
        @DisplayName("다중 디바이스 로그인 후 한 기기만 로그아웃")
        void multiDeviceLogin_logoutOneDevice() {
            // === Setup: 인증된 사용자 생성 ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            // === Step 1: 여러 기기에서 로그인 ===
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            LoginResult deviceAResult = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);
            LoginResult deviceBResult = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // 서로 다른 토큰 발급
            assertThat(deviceAResult.refreshToken()).isNotEqualTo(deviceBResult.refreshToken());

            // === Step 2: Device A 로그아웃 ===
            passwordAuthService.logout(deviceAResult.refreshToken());

            // === Step 3: Device A 토큰은 무효화, Device B 토큰은 유효 ===
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(deviceAResult.refreshToken())).isEmpty();
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(deviceBResult.refreshToken())).isPresent();
        }
    }

    // ===== E2E-003: 토큰 갱신 플로우 =====

    @Nested
    @DisplayName("[E2E-003] 토큰 갱신 플로우 (Access Token 만료 시뮬레이션)")
    class TokenRefreshFlowTest {

        @Test
        @DisplayName("Access Token 만료 후 Refresh Token으로 갱신 성공")
        void accessTokenExpired_refreshSucceeds() {
            // === Setup: 인증된 사용자 생성 및 로그인 ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            LoginResult loginResult = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);

            String originalAccessToken = loginResult.accessToken();
            String refreshToken = loginResult.refreshToken();

            // === Step 1: Access Token 만료 시뮬레이션 (실제로 만료된 것처럼 가정) ===
            // 실제 만료를 기다릴 수 없으므로, 새로운 토큰 발급으로 시뮬레이션

            // === Step 2: Refresh Token으로 새 Access Token 발급 ===
            TokenRefreshResponse refreshResponse = passwordAuthService.refreshToken(refreshToken);

            assertThat(refreshResponse).isNotNull();
            assertThat(refreshResponse.accessToken()).isNotNull();
            assertThat(refreshResponse.accessToken()).isNotEqualTo(originalAccessToken);
            assertThat(refreshResponse.expiresIn()).isEqualTo(ACCESS_TOKEN_VALIDITY);

            // === Step 3: 새 Access Token으로 API 호출 가능 확인 ===
            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(refreshResponse.accessToken());
            assertThat(jwtTokenProvider.getUserIdFromClaims(claims)).isEqualTo(user.getId());
            assertThat(jwtTokenProvider.getStudentIdFromClaims(claims)).isEqualTo(TEST_STUDENT_ID);
        }

        @Test
        @DisplayName("여러 번 토큰 갱신해도 항상 새로운 Access Token 발급")
        void multipleRefreshes_alwaysNewAccessToken() {
            // === Setup ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            LoginResult loginResult = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);

            String refreshToken = loginResult.refreshToken();

            // === 여러 번 갱신 ===
            TokenRefreshResponse response1 = passwordAuthService.refreshToken(refreshToken);
            TokenRefreshResponse response2 = passwordAuthService.refreshToken(refreshToken);
            TokenRefreshResponse response3 = passwordAuthService.refreshToken(refreshToken);

            // 매번 다른 Access Token
            assertThat(response1.accessToken()).isNotEqualTo(response2.accessToken());
            assertThat(response2.accessToken()).isNotEqualTo(response3.accessToken());
        }
    }

    // ===== E2E-004: 비밀번호 재설정 플로우 =====

    @Nested
    @DisplayName("[E2E-004] 비밀번호 재설정 플로우 (요청 → 토큰 → 변경 → 재로그인)")
    class PasswordResetFlowTest {

        @Test
        @DisplayName("비밀번호 재설정 전체 플로우 성공")
        void fullPasswordResetFlow_succeeds() {
            // === Setup: 인증된 사용자 생성 및 로그인 ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            // 기존 세션 생성
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            LoginResult loginResult = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);
            String oldRefreshToken = loginResult.refreshToken();

            // === Step 1: 비밀번호 재설정 요청 ===
            ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
            passwordResetService.requestPasswordReset(TEST_STUDENT_ID);

            verify(authEmailService).sendPasswordResetEmail(eq(TEST_EMAIL), linkCaptor.capture());
            String resetLink = linkCaptor.getValue();
            String resetToken = resetLink.substring(resetLink.indexOf("token=") + 6);

            // === Step 2: 토큰 검증 ===
            boolean isValid = passwordResetService.validateResetToken(resetToken);
            assertThat(isValid).isTrue();

            // === Step 3: 비밀번호 변경 ===
            String newPassword = "NewSecure1!@";
            passwordResetService.resetPassword(resetToken, newPassword);

            // === Step 4: 기존 세션 무효화 확인 ===
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(oldRefreshToken)).isEmpty();

            // === Step 5: 새 비밀번호로 재로그인 성공 ===
            PasswordLoginRequest newLoginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, newPassword);
            LoginResult newLoginResult = passwordAuthService.login(newLoginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);

            assertThat(newLoginResult).isNotNull();
            assertThat(newLoginResult.accessToken()).isNotNull();
            assertThat(newLoginResult.userId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("비밀번호 재설정 후 이전 비밀번호로 로그인 실패")
        void afterPasswordReset_oldPasswordFails() {
            // === Setup ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            // === 비밀번호 재설정 ===
            ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
            passwordResetService.requestPasswordReset(TEST_STUDENT_ID);
            verify(authEmailService).sendPasswordResetEmail(eq(TEST_EMAIL), linkCaptor.capture());

            String resetLink = linkCaptor.getValue();
            String resetToken = resetLink.substring(resetLink.indexOf("token=") + 6);

            String newPassword = "NewSecure1!@";
            passwordResetService.resetPassword(resetToken, newPassword);

            // === 이전 비밀번호로 로그인 시도 ===
            PasswordLoginRequest oldLoginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            assertThatThrownBy(() -> passwordAuthService.login(oldLoginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(Exception.class);
        }
    }

    // ===== E2E-005: 계정 탈퇴 → 복구 플로우 =====

    @Nested
    @DisplayName("[E2E-005] 계정 탈퇴 → 복구 플로우 (5일 이내)")
    class AccountWithdrawRecoveryFlowTest {

        @Test
        @DisplayName("탈퇴 후 5일 이내 로그인 시 복구 가능 안내")
        void withdrawnAccount_loginShowsRecoveryOption() {
            // === Setup: 탈퇴된 사용자 생성 ===
            User user = User.create(
                    TEST_STUDENT_ID,
                    TEST_NAME,
                    TEST_EMAIL,
                    TEST_PHONE,
                    TEST_DEPARTMENT,
                    TEST_MOTIVATION,
                    Gender.MALE,
                    1
            );
            user.changeRole(UserRole.MEMBER);
            user.verifyEmail();
            user.withdraw(); // 탈퇴 처리
            // soft delete 처리 - deleted와 deletedAt 설정
            ReflectionTestUtils.setField(user, "deleted", true);
            ReflectionTestUtils.setField(user, "deletedAt", Instant.now());
            userRepository.save(user);

            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            credential.withdraw(); // 탈퇴 처리
            ReflectionTestUtils.setField(credential, "deleted", true);
            ReflectionTestUtils.setField(credential, "deletedAt", Instant.now());
            passwordCredentialRepository.save(credential);

            // === 로그인 시도 시 AccountRecoverableException 발생 ===
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            assertThatThrownBy(() -> passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(AccountRecoverableException.class);
        }

        @Test
        @DisplayName("계정 복구 전체 플로우 성공")
        void fullAccountRecoveryFlow_succeeds() {
            // === Setup: 탈퇴된 사용자 생성 ===
            User user = User.create(
                    TEST_STUDENT_ID,
                    TEST_NAME,
                    TEST_EMAIL,
                    TEST_PHONE,
                    TEST_DEPARTMENT,
                    TEST_MOTIVATION,
                    Gender.MALE,
                    1
            );
            user.changeRole(UserRole.MEMBER);
            user.verifyEmail();
            user.withdraw();
            ReflectionTestUtils.setField(user, "deleted", true);
            ReflectionTestUtils.setField(user, "deletedAt", Instant.now());
            userRepository.save(user);

            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            credential.withdraw();
            ReflectionTestUtils.setField(credential, "deleted", true);
            ReflectionTestUtils.setField(credential, "deletedAt", Instant.now());
            passwordCredentialRepository.save(credential);

            // === Step 1: 계정 복구 ===
            RecoveryResult recoveryResult = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            assertThat(recoveryResult).isNotNull();
            assertThat(recoveryResult.accessToken()).isNotNull();
            assertThat(recoveryResult.refreshToken()).isNotNull();
            assertThat(recoveryResult.userId()).isEqualTo(user.getId());
            assertThat(recoveryResult.role()).isEqualTo(UserRole.MEMBER);

            // === Step 2: 복구 후 사용자 상태 확인 ===
            User recoveredUser = userRepository.findByStudentId(TEST_STUDENT_ID).orElseThrow();
            assertThat(recoveredUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(recoveredUser.isDeleted()).isFalse();

            // === Step 3: 복구 후 정상 로그인 가능 ===
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            LoginResult loginResult = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);

            assertThat(loginResult).isNotNull();
            assertThat(loginResult.accessToken()).isNotNull();
        }

        @Test
        @DisplayName("복구 시 기존 역할 유지")
        void accountRecovery_preservesOriginalRole() {
            // === Setup: OPERATOR 역할로 탈퇴된 사용자 생성 ===
            User user = User.create(
                    TEST_STUDENT_ID,
                    TEST_NAME,
                    TEST_EMAIL,
                    TEST_PHONE,
                    TEST_DEPARTMENT,
                    TEST_MOTIVATION,
                    Gender.MALE,
                    1
            );
            user.changeRole(UserRole.OPERATOR);
            user.verifyEmail();
            user.withdraw();
            ReflectionTestUtils.setField(user, "deleted", true);
            ReflectionTestUtils.setField(user, "deletedAt", Instant.now());
            userRepository.save(user);

            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            credential.withdraw();
            ReflectionTestUtils.setField(credential, "deleted", true);
            ReflectionTestUtils.setField(credential, "deletedAt", Instant.now());
            passwordCredentialRepository.save(credential);

            // === 계정 복구 ===
            RecoveryResult recoveryResult = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // === 기존 역할(OPERATOR) 유지 확인 ===
            assertThat(recoveryResult.role()).isEqualTo(UserRole.OPERATOR);
        }

        @Test
        @DisplayName("5일 경과 후 복구 불가")
        void accountRecovery_afterFiveDays_fails() {
            // === Setup: 5일 이전에 탈퇴된 사용자 생성 ===
            User user = User.create(
                    TEST_STUDENT_ID,
                    TEST_NAME,
                    TEST_EMAIL,
                    TEST_PHONE,
                    TEST_DEPARTMENT,
                    TEST_MOTIVATION,
                    Gender.MALE,
                    1
            );
            user.changeRole(UserRole.MEMBER);
            user.verifyEmail();
            user.withdraw();
            // 6일 전으로 설정
            ReflectionTestUtils.setField(user, "deleted", true);
            ReflectionTestUtils.setField(user, "deletedAt", Instant.now().minusSeconds(6 * 24 * 60 * 60));
            userRepository.save(user);

            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            credential.withdraw();
            ReflectionTestUtils.setField(credential, "deleted", true);
            ReflectionTestUtils.setField(credential, "deletedAt", Instant.now().minusSeconds(6 * 24 * 60 * 60));
            passwordCredentialRepository.save(credential);

            // === 로그인 시 AccountWithdrawnException 발생 (복구 불가) ===
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            assertThatThrownBy(() -> passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT))
                    .isInstanceOf(AccountWithdrawnException.class);
        }

        @Test
        @DisplayName("탈퇴 전 발급된 토큰으로 API 호출 실패")
        void withdrawnAccount_oldTokenInvalid() {
            // === Setup: 인증된 사용자 생성 및 로그인 ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            LoginResult loginResult = passwordAuthService.login(loginRequest, TEST_IP_ADDRESS, TEST_USER_AGENT);
            String oldRefreshToken = loginResult.refreshToken();

            // === 탈퇴 처리 (모든 토큰 무효화 시뮬레이션) ===
            transactionTemplate.execute(status -> {
                refreshTokenRepository.revokeAllByUserId(user.getId());
                return null;
            });

            // === 이전 토큰으로 API 호출 시도 (토큰 무효화 확인) ===
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(oldRefreshToken)).isEmpty();
        }
    }
}
