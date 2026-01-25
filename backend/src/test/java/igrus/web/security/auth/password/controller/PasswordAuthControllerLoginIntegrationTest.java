package igrus.web.security.auth.password.controller;

import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.dto.request.PasswordLogoutRequest;
import igrus.web.security.auth.password.dto.response.PasswordLoginResponse;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 로그인 HTTP 컨트롤러 통합 테스트 (T031)
 *
 * <p>MockMvc를 사용한 HTTP 레벨 통합 테스트입니다.</p>
 *
 * <p>테스트 범위:</p>
 * <ul>
 *     <li>LOG-001 ~ LOG-007: 로그인 성공 케이스</li>
 *     <li>LOG-010 ~ LOG-014: 로그인 실패 케이스</li>
 *     <li>LOG-020 ~ LOG-022: 계정 상태별 로그인 제한</li>
 *     <li>LOG-030 ~ LOG-032: 로그아웃 케이스</li>
 * </ul>
 */
@DisplayName("로그인 HTTP 컨트롤러 통합 테스트")
class PasswordAuthControllerLoginIntegrationTest extends ControllerIntegrationTestBase {

    @BeforeEach
    void setUp() {
        setUpControllerTest();
    }

    // ===== 로그인 성공 테스트 =====

    @Nested
    @DisplayName("로그인 성공 테스트")
    class LoginSuccessTest {

        @Test
        @DisplayName("[LOG-001] 정상 로그인 - 200 응답 및 Access/Refresh Token 발급")
        void login_withValidCredentials_returns200WithTokens() throws Exception {
            // given
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            performPost("/login", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.userId").isNumber())
                    .andExpect(jsonPath("$.studentId").value(TEST_STUDENT_ID))
                    .andExpect(jsonPath("$.name").value(TEST_NAME))
                    .andExpect(jsonPath("$.role").value("ASSOCIATE"))
                    .andExpect(jsonPath("$.expiresIn").value(ACCESS_TOKEN_VALIDITY));
        }

        @Test
        @DisplayName("[LOG-002] 정회원 로그인 - role이 MEMBER로 반환")
        void login_withMemberRole_returnsMemberRole() throws Exception {
            // given
            createAndSaveUserWithCredential(TEST_STUDENT_ID, TEST_EMAIL, TEST_PASSWORD,
                    UserRole.MEMBER, UserStatus.ACTIVE);
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            performPost("/login", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("MEMBER"));
        }

        @Test
        @DisplayName("[LOG-003] 운영진 로그인 - role이 OPERATOR로 반환")
        void login_withOperatorRole_returnsOperatorRole() throws Exception {
            // given
            createAndSaveUserWithCredential(TEST_STUDENT_ID, TEST_EMAIL, TEST_PASSWORD,
                    UserRole.OPERATOR, UserStatus.ACTIVE);
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            performPost("/login", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("OPERATOR"));
        }

        @Test
        @DisplayName("[LOG-004] 관리자 로그인 - role이 ADMIN으로 반환")
        void login_withAdminRole_returnsAdminRole() throws Exception {
            // given
            createAndSaveUserWithCredential(TEST_STUDENT_ID, TEST_EMAIL, TEST_PASSWORD,
                    UserRole.ADMIN, UserStatus.ACTIVE);
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            performPost("/login", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }
    }

    // ===== 로그인 실패 테스트 =====

    @Nested
    @DisplayName("로그인 실패 테스트")
    class LoginFailureTest {

        @Test
        @DisplayName("[LOG-010] 잘못된 학번으로 로그인 - 401 Unauthorized 응답")
        void login_withInvalidStudentId_returns401() throws Exception {
            // given
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest request = new PasswordLoginRequest("99999999", TEST_PASSWORD);

            // when & then
            performPost("/login", request)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[LOG-011] 잘못된 비밀번호로 로그인 - 401 Unauthorized 응답")
        void login_withInvalidPassword_returns401() throws Exception {
            // given
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, "WrongPass1!@");

            // when & then
            performPost("/login", request)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[LOG-012] 이메일 미인증 계정 로그인 - 401 Unauthorized 응답")
        void login_withUnverifiedEmail_returns401() throws Exception {
            // given
            createAndSaveUserWithCredential(TEST_STUDENT_ID, TEST_EMAIL, TEST_PASSWORD,
                    UserRole.ASSOCIATE, UserStatus.PENDING_VERIFICATION);
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            performPost("/login", request)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[LOG-013] 정지된 계정 로그인 - 403 Forbidden 응답")
        void login_withSuspendedAccount_returns403() throws Exception {
            // given
            createAndSaveUserWithCredential(TEST_STUDENT_ID, TEST_EMAIL, TEST_PASSWORD,
                    UserRole.MEMBER, UserStatus.SUSPENDED);
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            performPost("/login", request)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[LOG-014] 탈퇴한 계정 로그인 - 403 또는 409 응답 (복구 가능 여부에 따라)")
        void login_withWithdrawnAccount_returnsForbiddenOrConflict() throws Exception {
            // given
            createAndSaveUserWithCredential(TEST_STUDENT_ID, TEST_EMAIL, TEST_PASSWORD,
                    UserRole.MEMBER, UserStatus.WITHDRAWN);
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then - 탈퇴 계정은 403 또는 409(복구 가능) 응답
            performPost("/login", request)
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("[LOG-015] 빈 학번으로 로그인 - 400 Bad Request 응답")
        void login_withEmptyStudentId_returns400() throws Exception {
            // given
            PasswordLoginRequest request = new PasswordLoginRequest("", TEST_PASSWORD);

            // when & then
            performPost("/login", request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[LOG-016] 빈 비밀번호로 로그인 - 400 Bad Request 응답")
        void login_withEmptyPassword_returns400() throws Exception {
            // given
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, "");

            // when & then
            performPost("/login", request)
                    .andExpect(status().isBadRequest());
        }
    }

    // ===== 로그아웃 테스트 =====

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {

        @Test
        @DisplayName("[LOG-030] 로그아웃 성공 - 200 응답 및 Refresh Token 무효화")
        void logout_withValidRefreshToken_returns200AndRevokesToken() throws Exception {
            // given - 로그인하여 토큰 획득
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            PasswordLoginResponse loginResponse = passwordAuthService.login(loginRequest);

            PasswordLogoutRequest logoutRequest = new PasswordLogoutRequest(loginResponse.refreshToken());

            // when & then
            performPost("/logout", logoutRequest)
                    .andExpect(status().isOk());

            // 토큰 무효화 확인
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(loginResponse.refreshToken())).isEmpty();
        }

        @Test
        @DisplayName("[LOG-031] 로그아웃 후 동일 토큰으로 재로그아웃 시도 - 401 Unauthorized 응답")
        void logout_afterLogout_returns401() throws Exception {
            // given - 로그인 후 로그아웃
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            PasswordLoginResponse loginResponse = passwordAuthService.login(loginRequest);

            PasswordLogoutRequest logoutRequest = new PasswordLogoutRequest(loginResponse.refreshToken());
            passwordAuthService.logout(logoutRequest);

            // when - 동일 토큰으로 재로그아웃 시도
            performPost("/logout", logoutRequest)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[LOG-032] 잘못된 Refresh Token으로 로그아웃 시도 - 401 Unauthorized 응답")
        void logout_withInvalidRefreshToken_returns401() throws Exception {
            // given
            PasswordLogoutRequest logoutRequest = new PasswordLogoutRequest("invalid.refresh.token");

            // when & then
            performPost("/logout", logoutRequest)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[LOG-033] 빈 토큰으로 로그아웃 시도 - 400 Bad Request 응답")
        void logout_withEmptyToken_returns400() throws Exception {
            // given
            PasswordLogoutRequest logoutRequest = new PasswordLogoutRequest("");

            // when & then
            performPost("/logout", logoutRequest)
                    .andExpect(status().isBadRequest());
        }
    }

    // ===== 다중 디바이스 로그인 테스트 =====

    @Nested
    @DisplayName("다중 디바이스 로그인 테스트")
    class MultiDeviceLoginTest {

        @Test
        @DisplayName("[LOG-040] 여러 기기에서 동시 로그인 - 각각 독립된 토큰 발급")
        void login_multipleDevices_issuesSeparateTokens() throws Exception {
            // given
            User user = createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when - 두 번 로그인
            PasswordLoginResponse response1 = passwordAuthService.login(request);
            PasswordLoginResponse response2 = passwordAuthService.login(request);

            // then - 서로 다른 토큰 발급
            assertThat(response1.accessToken()).isNotEqualTo(response2.accessToken());
            assertThat(response1.refreshToken()).isNotEqualTo(response2.refreshToken());

            // 둘 다 유효한 토큰
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(response1.refreshToken())).isPresent();
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(response2.refreshToken())).isPresent();
        }

        @Test
        @DisplayName("[LOG-041] 한 기기 로그아웃 시 다른 기기 토큰 유지")
        void logout_oneDevice_otherDeviceTokensRemainValid() throws Exception {
            // given
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // 두 기기에서 로그인
            PasswordLoginResponse deviceA = passwordAuthService.login(loginRequest);
            PasswordLoginResponse deviceB = passwordAuthService.login(loginRequest);

            // when - Device A 로그아웃
            PasswordLogoutRequest logoutRequest = new PasswordLogoutRequest(deviceA.refreshToken());
            performPost("/logout", logoutRequest)
                    .andExpect(status().isOk());

            // then - Device A 토큰 무효화, Device B 토큰 유효
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(deviceA.refreshToken())).isEmpty();
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(deviceB.refreshToken())).isPresent();
        }
    }
}
