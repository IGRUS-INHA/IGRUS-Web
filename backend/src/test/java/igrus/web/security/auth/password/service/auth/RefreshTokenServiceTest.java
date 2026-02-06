package igrus.web.security.auth.password.service.auth;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.exception.token.RefreshTokenExpiredException;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.common.exception.token.RefreshTokenTheftException;
import igrus.web.security.auth.password.dto.internal.TokenRotationResult;
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

@DisplayName("RefreshTokenService 토큰 갱신 통합 테스트")
class RefreshTokenServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final long ACCESS_TOKEN_VALIDITY = 300000L; // 5분
    private static final long REFRESH_TOKEN_VALIDITY = 259200000L; // 3일
    private static final long GRACE_PERIOD_MILLIS = 10000L; // 10초
    private static final String TEST_TOKEN_FAMILY = "test-family-uuid";

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(refreshTokenService, "accessTokenValidity", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenValidity", REFRESH_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(refreshTokenService, "gracePeriodMillis", GRACE_PERIOD_MILLIS);
    }

    private User createAndSaveTestUser() {
        return createAndSaveUser("20231234", "test@inha.edu", UserRole.MEMBER);
    }

    private RefreshToken createAndSaveValidRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.create(user, token, REFRESH_TOKEN_VALIDITY, TEST_TOKEN_FAMILY);
        return refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken createAndSaveExpiredRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.create(user, token, REFRESH_TOKEN_VALIDITY, TEST_TOKEN_FAMILY);
        // 리플렉션으로 expiresAt을 과거 시간으로 설정
        ReflectionTestUtils.setField(refreshToken, "expiresAt", Instant.now().minusMillis(1000L));
        return refreshTokenRepository.save(refreshToken);
    }

    @Nested
    @DisplayName("토큰 갱신 및 로테이션 성공")
    class TokenRefreshSuccessTest {

        @Test
        @DisplayName("유효한 Refresh Token으로 갱신 성공 - 새 Access Token 반환 [TKN-001]")
        void refreshToken_WithValidToken_ReturnsNewAccessToken() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRotationResult result = refreshTokenService.refreshToken(refreshTokenString);

            // then
            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("갱신된 Access Token의 유효기간 확인 [TKN-002]")
        void refreshToken_WithValidToken_ReturnsCorrectExpiresIn() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRotationResult result = refreshTokenService.refreshToken(refreshTokenString);

            // then
            assertThat(result.accessTokenValidity()).isEqualTo(ACCESS_TOKEN_VALIDITY);
        }

        @Test
        @DisplayName("갱신된 Access Token에 사용자 정보 포함 [TKN-003]")
        void refreshToken_WithValidToken_AccessTokenContainsUserInfo() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRotationResult result = refreshTokenService.refreshToken(refreshTokenString);

            // then
            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(result.accessToken());
            assertThat(jwtTokenProvider.getUserIdFromClaims(claims)).isEqualTo(user.getId());
            assertThat(jwtTokenProvider.getStudentIdFromClaims(claims)).isEqualTo(user.getStudentId());
            assertThat(jwtTokenProvider.getRoleFromClaims(claims)).isEqualTo(user.getRole().name());
        }

        @Test
        @DisplayName("토큰 로테이션 - 새 Refresh Token 발급 [TKN-005]")
        void refreshToken_WithValidToken_ReturnsNewRefreshToken() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRotationResult result = refreshTokenService.refreshToken(refreshTokenString);

            // then
            assertThat(result.newRefreshToken()).isNotNull().isNotEmpty();
            assertThat(result.newRefreshToken()).isNotEqualTo(refreshTokenString);
        }

        @Test
        @DisplayName("토큰 로테이션 후 기존 토큰 폐기 확인 [TKN-006]")
        void refreshToken_AfterRotation_OldTokenRevoked() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            refreshTokenService.refreshToken(refreshTokenString);

            // then
            RefreshToken oldToken = refreshTokenRepository.findByToken(refreshTokenString).orElseThrow();
            assertThat(oldToken.isRevoked()).isTrue();
            assertThat(oldToken.getRevokedAt()).isNotNull();
            assertThat(oldToken.getReplacedByToken()).isNotNull();
        }

        @Test
        @DisplayName("토큰 로테이션 후 새 토큰이 같은 패밀리 [TKN-007]")
        void refreshToken_AfterRotation_SameTokenFamily() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when
            TokenRotationResult result = refreshTokenService.refreshToken(refreshTokenString);

            // then
            RefreshToken newToken = refreshTokenRepository.findByToken(result.newRefreshToken()).orElseThrow();
            assertThat(newToken.getTokenFamily()).isEqualTo(TEST_TOKEN_FAMILY);
        }

        @Test
        @DisplayName("연쇄 토큰 로테이션 - 새 토큰으로 다시 갱신 가능 [TKN-008]")
        void refreshToken_ChainedRotation_Works() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // when - 1차 갱신
            TokenRotationResult result1 = refreshTokenService.refreshToken(refreshTokenString);
            // when - 2차 갱신 (새 토큰으로)
            TokenRotationResult result2 = refreshTokenService.refreshToken(result1.newRefreshToken());

            // then
            assertThat(result2.accessToken()).isNotNull().isNotEmpty();
            assertThat(result2.newRefreshToken()).isNotEqualTo(result1.newRefreshToken());
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

            // when & then
            assertThatThrownBy(() -> refreshTokenService.refreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenExpiredException.class);
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token으로 갱신 시도 시 예외 발생 [TKN-011]")
        void refreshToken_WithInvalidToken_ThrowsException() {
            // given
            String tokenString = "invalid-refresh-token";

            // when & then
            assertThatThrownBy(() -> refreshTokenService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("변조된 Refresh Token으로 갱신 시도 시 예외 발생 [TKN-012]")
        void refreshToken_WithTamperedToken_ThrowsException() {
            // given
            String tokenString = "tampered-token-payload-modified";

            // when & then
            assertThatThrownBy(() -> refreshTokenService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("빈 Refresh Token으로 갱신 시도 시 예외 발생 [TKN-013]")
        void refreshToken_WithEmptyToken_ThrowsException() {
            // given
            String tokenString = "";

            // when & then
            assertThatThrownBy(() -> refreshTokenService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }
    }

    @Nested
    @DisplayName("토큰 탈취 감지")
    class TokenTheftDetectionTest {

        @Test
        @DisplayName("폐기된 토큰 사용 시 (Grace Period 밖) 탈취 감지 예외 발생 [TKN-030]")
        void refreshToken_WithRevokedTokenOutsideGracePeriod_ThrowsTheftException() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            RefreshToken refreshToken = createAndSaveValidRefreshToken(user, refreshTokenString);

            // 토큰 폐기 후 Grace Period 지나게 설정
            refreshToken.revoke();
            ReflectionTestUtils.setField(refreshToken, "revokedAt", Instant.now().minusSeconds(11));
            refreshTokenRepository.save(refreshToken);

            // when & then
            assertThatThrownBy(() -> refreshTokenService.refreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenTheftException.class);
        }

        @Test
        @DisplayName("탈취 감지 시 같은 패밀리의 모든 토큰 무효화 [TKN-031]")
        void refreshToken_TheftDetected_RevokesEntireFamily() {
            // given
            User user = createAndSaveTestUser();
            String oldRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());
            RefreshToken oldToken = createAndSaveValidRefreshToken(user, oldRefreshToken);

            // 새 토큰을 같은 패밀리로 생성 (로테이션 시뮬레이션)
            String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());
            RefreshToken newToken = RefreshToken.create(user, newRefreshToken, REFRESH_TOKEN_VALIDITY, TEST_TOKEN_FAMILY);
            refreshTokenRepository.save(newToken);

            // 기존 토큰을 폐기 (Grace Period 밖)
            oldToken.revoke();
            ReflectionTestUtils.setField(oldToken, "revokedAt", Instant.now().minusSeconds(11));
            refreshTokenRepository.save(oldToken);

            // when - 폐기된 기존 토큰으로 갱신 시도 (탈취 시나리오)
            assertThatThrownBy(() -> refreshTokenService.refreshToken(oldRefreshToken))
                    .isInstanceOf(RefreshTokenTheftException.class);

            // then - 같은 패밀리의 새 토큰도 무효화됨
            RefreshToken revokedNewToken = refreshTokenRepository.findByToken(newRefreshToken).orElseThrow();
            assertThat(revokedNewToken.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("Grace Period 내 폐기된 토큰 사용 시 정상 응답 [TKN-032]")
        void refreshToken_WithRevokedTokenWithinGracePeriod_Succeeds() {
            // given
            User user = createAndSaveTestUser();
            String oldRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());
            RefreshToken oldToken = createAndSaveValidRefreshToken(user, oldRefreshToken);

            // 새 토큰을 같은 패밀리로 생성 (로테이션 완료 상태)
            String activeRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());
            RefreshToken activeToken = RefreshToken.create(user, activeRefreshToken, REFRESH_TOKEN_VALIDITY, TEST_TOKEN_FAMILY);
            refreshTokenRepository.save(activeToken);

            // 기존 토큰을 방금 폐기 (Grace Period 내)
            oldToken.rotateWith(activeRefreshToken);
            refreshTokenRepository.save(oldToken);

            // when - Grace Period 내에 폐기된 토큰으로 갱신 시도 (동시 탭 시나리오)
            TokenRotationResult result = refreshTokenService.refreshToken(oldRefreshToken);

            // then - 정상적으로 액세스 토큰만 발급 (리프레시 토큰은 null)
            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isNotNull().isNotEmpty();
            assertThat(result.newRefreshToken()).isNull();
        }

        @Test
        @DisplayName("폐기되고 만료된 토큰은 Grace Period 내여도 만료 예외 발생 [TKN-034]")
        void refreshToken_WithRevokedAndExpiredTokenWithinGracePeriod_ThrowsExpiredException() {
            // given
            User user = createAndSaveTestUser();
            String oldRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());
            RefreshToken oldToken = createAndSaveValidRefreshToken(user, oldRefreshToken);

            // 토큰을 방금 폐기하고 (Grace Period 내) 만료도 시킴
            oldToken.revoke();
            ReflectionTestUtils.setField(oldToken, "expiresAt", Instant.now().minusMillis(1000L));
            refreshTokenRepository.save(oldToken);

            // when & then - 만료 체크가 Grace Period 체크보다 먼저 실행됨
            assertThatThrownBy(() -> refreshTokenService.refreshToken(oldRefreshToken))
                    .isInstanceOf(RefreshTokenExpiredException.class);
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
            assertThatThrownBy(() -> refreshTokenService.refreshToken(tokenString))
                    .isInstanceOf(RefreshTokenInvalidException.class);
        }

        @Test
        @DisplayName("모든 토큰이 무효화된 경우 탈취 감지 [TKN-021]")
        void refreshToken_WhenAllTokensRevoked_ThrowsTheftException() {
            // given
            User user = createAndSaveTestUser();
            String refreshTokenString = jwtTokenProvider.createRefreshToken(user.getId());
            createAndSaveValidRefreshToken(user, refreshTokenString);

            // 모든 토큰 무효화 (비밀번호 재설정 등의 시나리오)
            transactionTemplate.executeWithoutResult(status ->
                    refreshTokenRepository.revokeAllByUserId(user.getId())
            );

            // revokedAt이 방금 설정되었으므로 Grace Period 밖으로 설정
            RefreshToken revokedToken = refreshTokenRepository.findByToken(refreshTokenString).orElseThrow();
            ReflectionTestUtils.setField(revokedToken, "revokedAt", Instant.now().minusSeconds(11));
            refreshTokenRepository.save(revokedToken);

            // when & then
            assertThatThrownBy(() -> refreshTokenService.refreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenTheftException.class);
        }
    }
}
