package igrus.web.security.auth.password.controller;

import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.dto.request.TokenRefreshRequest;
import igrus.web.security.auth.password.dto.response.PasswordLoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 토큰 갱신 HTTP 컨트롤러 통합 테스트 (T037)
 *
 * <p>MockMvc를 사용한 HTTP 레벨 통합 테스트입니다.</p>
 *
 * <p>테스트 범위:</p>
 * <ul>
 *     <li>TOK-001 ~ TOK-003: 토큰 갱신 성공 케이스</li>
 *     <li>TOK-010 ~ TOK-015: 토큰 갱신 실패 케이스</li>
 * </ul>
 */
@DisplayName("토큰 갱신 HTTP 컨트롤러 통합 테스트")
class PasswordAuthControllerTokenIntegrationTest extends ControllerIntegrationTestBase {

    @BeforeEach
    void setUp() {
        setUpControllerTest();
    }

    // ===== 토큰 갱신 성공 테스트 =====

    @Nested
    @DisplayName("토큰 갱신 성공 테스트")
    class TokenRefreshSuccessTest {

        @Test
        @DisplayName("[TOK-001] 유효한 Refresh Token으로 갱신 - 200 응답 및 새 Access Token 발급")
        void refreshToken_withValidToken_returns200WithNewAccessToken() throws Exception {
            // given - 로그인하여 토큰 획득
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            PasswordLoginResponse loginResponse = passwordAuthService.login(loginRequest);

            String originalAccessToken = loginResponse.accessToken();
            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(loginResponse.refreshToken());

            // when & then
            performPost("/refresh", refreshRequest)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.expiresIn").value(ACCESS_TOKEN_VALIDITY));
        }

        @Test
        @DisplayName("[TOK-002] 토큰 갱신 시 새로운 Access Token 발급 확인")
        void refreshToken_generatesNewAccessToken() throws Exception {
            // given
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            PasswordLoginResponse loginResponse = passwordAuthService.login(loginRequest);

            String originalAccessToken = loginResponse.accessToken();
            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(loginResponse.refreshToken());

            // when
            var newTokenResponse = passwordAuthService.refreshToken(refreshRequest);

            // then - 새로운 Access Token이 발급됨
            assertThat(newTokenResponse.accessToken()).isNotEqualTo(originalAccessToken);
        }

        @Test
        @DisplayName("[TOK-003] 여러 번 토큰 갱신 시 매번 새로운 Access Token 발급")
        void refreshToken_multipleRefreshes_generatesUniqueTokens() throws Exception {
            // given
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            PasswordLoginResponse loginResponse = passwordAuthService.login(loginRequest);

            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(loginResponse.refreshToken());

            // when
            var response1 = passwordAuthService.refreshToken(refreshRequest);
            var response2 = passwordAuthService.refreshToken(refreshRequest);
            var response3 = passwordAuthService.refreshToken(refreshRequest);

            // then - 모든 Access Token이 서로 다름
            assertThat(response1.accessToken())
                    .isNotEqualTo(response2.accessToken())
                    .isNotEqualTo(response3.accessToken());
            assertThat(response2.accessToken()).isNotEqualTo(response3.accessToken());
        }
    }

    // ===== 토큰 갱신 실패 테스트 =====

    @Nested
    @DisplayName("토큰 갱신 실패 테스트")
    class TokenRefreshFailureTest {

        @Test
        @DisplayName("[TOK-010] 만료된 Refresh Token으로 갱신 - 401 Unauthorized 응답")
        void refreshToken_withExpiredToken_returns401() throws Exception {
            // given - 로그인하여 토큰 획득
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            PasswordLoginResponse loginResponse = passwordAuthService.login(loginRequest);

            // Refresh Token 만료 시뮬레이션
            RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(loginResponse.refreshToken())
                    .orElseThrow();
            ReflectionTestUtils.setField(refreshToken, "expiresAt", Instant.now().minusSeconds(60));
            refreshTokenRepository.save(refreshToken);

            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(loginResponse.refreshToken());

            // when & then
            performPost("/refresh", refreshRequest)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[TOK-011] 잘못된 형식의 Refresh Token으로 갱신 - 401 Unauthorized 응답")
        void refreshToken_withMalformedToken_returns401() throws Exception {
            // given
            TokenRefreshRequest refreshRequest = new TokenRefreshRequest("invalid.malformed.token");

            // when & then
            performPost("/refresh", refreshRequest)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[TOK-012] DB에 없는 Refresh Token으로 갱신 - 401 Unauthorized 응답")
        void refreshToken_withNonExistentToken_returns401() throws Exception {
            // given - 유효한 형식이지만 DB에 없는 토큰
            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"
            );

            // when & then
            performPost("/refresh", refreshRequest)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[TOK-013] 로그아웃된 (무효화된) Refresh Token으로 갱신 - 401 Unauthorized 응답")
        void refreshToken_withRevokedToken_returns401() throws Exception {
            // given - 로그인 후 로그아웃
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            PasswordLoginResponse loginResponse = passwordAuthService.login(loginRequest);

            // 로그아웃으로 토큰 무효화
            passwordAuthService.logout(new igrus.web.security.auth.password.dto.request.PasswordLogoutRequest(
                    loginResponse.refreshToken()));

            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(loginResponse.refreshToken());

            // when & then
            performPost("/refresh", refreshRequest)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[TOK-014] 빈 Refresh Token으로 갱신 - 400 Bad Request 응답")
        void refreshToken_withEmptyToken_returns400() throws Exception {
            // given
            TokenRefreshRequest refreshRequest = new TokenRefreshRequest("");

            // when & then
            performPost("/refresh", refreshRequest)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[TOK-015] null Refresh Token으로 갱신 - 400 Bad Request 응답")
        void refreshToken_withNullToken_returns400() throws Exception {
            // given - JSON에서 refreshToken 필드가 없는 경우
            String requestJson = "{}";

            // when & then
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post(API_BASE_PATH + "/refresh")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }
    }

    // ===== 토큰 갱신 후 검증 테스트 =====

    @Nested
    @DisplayName("토큰 갱신 후 검증 테스트")
    class TokenRefreshValidationTest {

        @Test
        @DisplayName("[TOK-020] 갱신된 Access Token의 사용자 정보 검증")
        void refreshToken_newTokenContainsCorrectUserInfo() throws Exception {
            // given
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            PasswordLoginResponse loginResponse = passwordAuthService.login(loginRequest);

            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(loginResponse.refreshToken());

            // when
            var newTokenResponse = passwordAuthService.refreshToken(refreshRequest);

            // then - 새 토큰에서 사용자 정보 추출
            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(newTokenResponse.accessToken());
            assertThat(jwtTokenProvider.getStudentIdFromClaims(claims)).isEqualTo(TEST_STUDENT_ID);
            assertThat(jwtTokenProvider.getUserIdFromClaims(claims)).isEqualTo(loginResponse.userId());
        }

        @Test
        @DisplayName("[TOK-021] 갱신된 Access Token의 만료 시간 검증")
        void refreshToken_newTokenHasCorrectExpiry() throws Exception {
            // given
            createAndSaveDefaultUserWithCredential();
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            PasswordLoginResponse loginResponse = passwordAuthService.login(loginRequest);

            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(loginResponse.refreshToken());

            // when & then
            performPost("/refresh", refreshRequest)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expiresIn").value(ACCESS_TOKEN_VALIDITY));
        }
    }
}
