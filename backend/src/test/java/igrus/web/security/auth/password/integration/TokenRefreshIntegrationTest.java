package igrus.web.security.auth.password.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.exception.token.RefreshTokenExpiredException;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.password.dto.response.TokenRefreshResponse;
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 토큰 갱신 통합 테스트 (18개 테스트 케이스)
 *
 * <p>테스트 케이스 문서: docs/test-case/auth/token-test-cases.md</p>
 *
 * <p>테스트 범위:</p>
 * <ul>
 *     <li>TKN-001 ~ TKN-004: 토큰 갱신 성공</li>
 *     <li>TKN-010 ~ TKN-014: 토큰 갱신 실패</li>
 *     <li>TKN-020 ~ TKN-024: 계정 상태 변경 시 토큰 처리</li>
 *     <li>TKN-030 ~ TKN-032: 토큰 보안</li>
 * </ul>
 */
@DisplayName("토큰 갱신 통합 테스트")
class TokenRefreshIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private PasswordAuthService passwordAuthService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final long ACCESS_TOKEN_VALIDITY = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7일

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(passwordAuthService, "accessTokenValidity", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(passwordAuthService, "refreshTokenValidity", REFRESH_TOKEN_VALIDITY);
    }

    private User createAndSaveTestUser() {
        return createAndSaveUser("20231234", "test@inha.edu", UserRole.MEMBER);
    }

    private RefreshToken createAndSaveValidRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.create(user, token, REFRESH_TOKEN_VALIDITY);
        return refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken createAndSaveExpiredRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.create(user, token, REFRESH_TOKEN_VALIDITY);
        // 리플렉션으로 expiresAt을 과거 시간으로 설정
        ReflectionTestUtils.setField(refreshToken, "expiresAt", Instant.now().minusMillis(1000L));
        return refreshTokenRepository.save(refreshToken);
    }

    // ===== 2.1 토큰 갱신 성공 테스트 =====

    @Nested
    @DisplayName("토큰 갱신 성공 테스트")
    class TokenRefreshSuccessTest {

        @Test
        @DisplayName("[TKN-001] 유효한 Refresh Token으로 갱신 성공")
        void refreshToken_withValidToken_returnsNewAccessToken() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(refreshTokenString);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.accessToken()).isNotEmpty();
        }

        @Test
        @DisplayName("[TKN-002] 갱신된 Access Token 1시간 유효")
        void refreshToken_withValidToken_returnsCorrectExpiresIn() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(refreshTokenString);

            // then
            assertThat(response.expiresIn()).isEqualTo(ACCESS_TOKEN_VALIDITY);
        }

        @Test
        @DisplayName("[TKN-003] 갱신된 Access Token으로 API 호출 성공 - 사용자 정보 포함")
        void refreshToken_withValidToken_accessTokenContainsUserInfo() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(refreshTokenString);

            // then
            assertThat(response.accessToken()).isNotNull();
            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(response.accessToken());
            assertThat(jwtTokenProvider.getUserIdFromClaims(claims)).isEqualTo(user.getId());
            assertThat(jwtTokenProvider.getStudentIdFromClaims(claims)).isEqualTo(user.getStudentId());
            assertThat(jwtTokenProvider.getRoleFromClaims(claims)).isEqualTo(user.getRole().name());
        }

        @Test
        @DisplayName("[TKN-004] Access Token 만료 전 갱신 가능")
        void refreshToken_beforeAccessTokenExpires_returnsNewAccessToken() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(refreshTokenString);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotNull();
        }

        @Test
        @DisplayName("[TKN-003] 여러 번 갱신해도 매번 새로운 Access Token 발급")
        void refreshToken_multipleTimes_generatesNewAccessTokenEachTime() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRefreshResponse response1 = passwordAuthService.refreshToken(refreshTokenString);
            TokenRefreshResponse response2 = passwordAuthService.refreshToken(refreshTokenString);

            // then
            assertThat(response1.accessToken()).isNotEqualTo(response2.accessToken());
        }
    }

    // ===== 2.2 토큰 갱신 실패 테스트 =====

    @Nested
    @DisplayName("토큰 갱신 실패 테스트")
    class TokenRefreshFailureTest {

        @Test
        @DisplayName("[TKN-010] 만료된 Refresh Token으로 갱신 시도 시 예외 발생")
        void refreshToken_withExpiredToken_throwsException() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveExpiredRefreshToken(user, refreshTokenString);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenExpiredException.class);
        }

        @Test
        @DisplayName("[TKN-011] 유효하지 않은 Refresh Token으로 갱신 시도 시 예외 발생")
        void refreshToken_withInvalidToken_throwsException() {
            // given
            String tokenString = "invalid-refresh-token";

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("[TKN-012] 변조된 Refresh Token으로 갱신 시도 시 예외 발생")
        void refreshToken_withTamperedToken_throwsException() {
            // given
            String tokenString = "tampered-token-payload-modified";

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("[TKN-013] 빈 Refresh Token으로 갱신 시도 시 예외 발생")
        void refreshToken_withEmptyToken_throwsException() {
            // given
            String tokenString = "";

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("[TKN-014] 로그아웃된 Refresh Token으로 갱신 시도 시 예외 발생")
        void refreshToken_withRevokedToken_throwsException() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            RefreshToken refreshToken = createAndSaveValidRefreshToken(user, refreshTokenString);

            // 토큰 무효화
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("[TKN-011] DB에 존재하지 않는 토큰으로 갱신 시도 시 예외 발생")
        void refreshToken_withNonExistentToken_throwsException() {
            // given - DB에 저장하지 않은 유효한 형식의 토큰
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            // DB에 저장하지 않음

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }
    }

    // ===== 2.3 계정 상태 변경 시 토큰 처리 테스트 =====

    @Nested
    @DisplayName("계정 상태 변경 시 토큰 처리 테스트")
    class AccountStatusTokenTest {

        @Test
        @DisplayName("[TKN-020] 토큰이 존재하지 않는 경우 갱신 실패")
        void refreshToken_whenTokenNotExists_throwsException() {
            // given
            String tokenString = "non-existent-token";

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("[TKN-021] 모든 토큰이 무효화된 경우 갱신 실패")
        void refreshToken_whenAllTokensRevoked_throwsException() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // 모든 토큰 무효화 (비밀번호 재설정 등의 시나리오)
            transactionTemplate.executeWithoutResult(status ->
                    refreshTokenRepository.revokeAllByUserId(user.getId())
            );

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("[TKN-024] 모든 토큰 무효화 후 새 토큰 발급 가능 확인")
        void refreshToken_afterRevokeAll_newTokenWorks() {
            // given
            User user = createAndSaveTestUser();
            String oldRefreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, oldRefreshTokenString);

            // 모든 토큰 무효화
            transactionTemplate.executeWithoutResult(status ->
                    refreshTokenRepository.revokeAllByUserId(user.getId())
            );

            // 새 토큰 발급
            String newRefreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, newRefreshTokenString);

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(newRefreshTokenString);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotNull();
        }
    }

    // ===== 2.4 토큰 보안 테스트 =====

    @Nested
    @DisplayName("토큰 보안 테스트")
    class TokenSecurityTest {

        @Test
        @DisplayName("[TKN-030] Access Token 서명 검증")
        void accessToken_withValidSignature_isValid() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(refreshTokenString);

            // then - 유효한 토큰은 검증 통과
            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(response.accessToken());
            assertThat(claims).isNotNull();
        }

        @Test
        @DisplayName("[TKN-032] Access Token에 역할 정보 포함")
        void accessToken_containsRoleInformation() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(refreshTokenString);

            // then
            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(response.accessToken());
            String role = jwtTokenProvider.getRoleFromClaims(claims);
            assertThat(role).isEqualTo(UserRole.MEMBER.name());
        }

        @Test
        @DisplayName("[TKN-032] Access Token에 사용자 ID 포함")
        void accessToken_containsUserId() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(refreshTokenString);

            // then
            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(response.accessToken());
            Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
            assertThat(userId).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("[TKN-032] Access Token에 학번 정보 포함")
        void accessToken_containsStudentId() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(refreshTokenString);

            // then
            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(response.accessToken());
            String studentId = jwtTokenProvider.getStudentIdFromClaims(claims);
            assertThat(studentId).isEqualTo(user.getStudentId());
        }
    }
}
