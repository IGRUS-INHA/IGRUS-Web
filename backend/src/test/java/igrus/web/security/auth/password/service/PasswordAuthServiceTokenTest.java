package igrus.web.security.auth.password.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.exception.token.RefreshTokenExpiredException;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.password.dto.response.TokenRefreshResponse;
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

@DisplayName("PasswordAuthService 토큰 갱신 통합 테스트")
class PasswordAuthServiceTokenTest extends ServiceIntegrationTestBase {

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

    @Nested
    @DisplayName("토큰 갱신 성공")
    class TokenRefreshSuccessTest {

        @Test
        @DisplayName("유효한 Refresh Token으로 갱신 성공 [TKN-001]")
        void refreshToken_WithValidToken_ReturnsNewAccessToken() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            String tokenString = refreshTokenString;

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(tokenString);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.accessToken()).isNotEmpty();
        }

        @Test
        @DisplayName("갱신된 Access Token의 유효기간 확인 [TKN-002]")
        void refreshToken_WithValidToken_ReturnsCorrectExpiresIn() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            String tokenString = refreshTokenString;

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(tokenString);

            // then
            assertThat(response.expiresIn()).isEqualTo(ACCESS_TOKEN_VALIDITY);
        }

        @Test
        @DisplayName("갱신된 Access Token에 사용자 정보 포함 [TKN-003]")
        void refreshToken_WithValidToken_AccessTokenContainsUserInfo() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            String tokenString = refreshTokenString;

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(tokenString);

            // then
            assertThat(response.accessToken()).isNotNull();
            // 실제 Access Token 검증
            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(response.accessToken());
            assertThat(jwtTokenProvider.getUserIdFromClaims(claims)).isEqualTo(user.getId());
            assertThat(jwtTokenProvider.getStudentIdFromClaims(claims)).isEqualTo(user.getStudentId());
            assertThat(jwtTokenProvider.getRoleFromClaims(claims)).isEqualTo(user.getRole().name());
        }

        @Test
        @DisplayName("Access Token 만료 전 갱신 가능 [TKN-004]")
        void refreshToken_BeforeAccessTokenExpires_ReturnsNewAccessToken() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            String tokenString = refreshTokenString;

            // when
            TokenRefreshResponse response = passwordAuthService.refreshToken(tokenString);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotNull();
        }
    }

    @Nested
    @DisplayName("토큰 갱신 실패")
    class TokenRefreshFailureTest {

        @Test
        @DisplayName("만료된 Refresh Token으로 갱신 시도 시 예외 발생 [TKN-010]")
        void refreshToken_WithExpiredToken_ThrowsException() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveExpiredRefreshToken(user, refreshTokenString);

            String tokenString = refreshTokenString;

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenExpiredException.class);
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token으로 갱신 시도 시 예외 발생 [TKN-011]")
        void refreshToken_WithInvalidToken_ThrowsException() {
            // given
            String tokenString = "invalid-refresh-token";

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("변조된 Refresh Token으로 갱신 시도 시 예외 발생 [TKN-012]")
        void refreshToken_WithTamperedToken_ThrowsException() {
            // given
            String tokenString = "tampered-token-payload-modified";

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("빈 Refresh Token으로 갱신 시도 시 예외 발생 [TKN-013]")
        void refreshToken_WithEmptyToken_ThrowsException() {
            // given
            String tokenString = "";

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("로그아웃된 Refresh Token으로 갱신 시도 시 예외 발생 [TKN-014]")
        void refreshToken_WithRevokedToken_ThrowsException() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            RefreshToken refreshToken = createAndSaveValidRefreshToken(user, refreshTokenString);

            // 토큰 무효화
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);

            String tokenString = refreshTokenString;

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }
    }

    @Nested
    @DisplayName("계정 상태 변경 시 토큰 처리")
    class AccountStatusTokenTest {

        @Test
        @DisplayName("토큰이 존재하지 않는 경우 갱신 실패 [TKN-020]")
        void refreshToken_WhenTokenNotExists_ThrowsException() {
            // given
            String tokenString = "non-existent-token";

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("모든 토큰이 무효화된 경우 갱신 실패 [TKN-021]")
        void refreshToken_WhenAllTokensRevoked_ThrowsException() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // 모든 토큰 무효화 (비밀번호 재설정 등의 시나리오)
            transactionTemplate.executeWithoutResult(status ->
                    refreshTokenRepository.revokeAllByUserId(user.getId())
            );

            String tokenString = refreshTokenString;

            // when & then
            assertThatThrownBy(() -> passwordAuthService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }
    }
}
