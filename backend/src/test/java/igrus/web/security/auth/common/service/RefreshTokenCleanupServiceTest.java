package igrus.web.security.auth.common.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshTokenCleanupService 통합 테스트")
class RefreshTokenCleanupServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private RefreshTokenCleanupService refreshTokenCleanupService;

    private User testUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        testUser = createAndSaveUser("20231234", "test@inha.edu", UserRole.MEMBER);
    }

    private RefreshToken createAndSaveRefreshToken(User user, Instant expiresAt) {
        RefreshToken token = RefreshToken.create(user, "token-" + System.nanoTime(), 3600000L);
        ReflectionTestUtils.setField(token, "expiresAt", expiresAt);
        return refreshTokenRepository.save(token);
    }

    @Nested
    @DisplayName("만료된 토큰 삭제 테스트")
    class DeleteExpiredTokensTest {

        @Test
        @DisplayName("만료된 토큰만 삭제되고 유효한 토큰은 유지된다")
        void deleteExpiredTokens_onlyDeletesExpiredTokens() {
            // given
            Instant now = Instant.now();
            Instant expired = now.minus(1, ChronoUnit.HOURS);
            Instant valid = now.plus(1, ChronoUnit.HOURS);

            RefreshToken expiredToken = createAndSaveRefreshToken(testUser, expired);
            RefreshToken validToken = createAndSaveRefreshToken(testUser, valid);

            // when
            int deletedCount = refreshTokenCleanupService.deleteExpiredTokens();

            // then
            assertThat(deletedCount).isEqualTo(1);
            assertThat(refreshTokenRepository.findById(expiredToken.getId())).isEmpty();
            assertThat(refreshTokenRepository.findById(validToken.getId())).isPresent();
        }

        @Test
        @DisplayName("만료된 토큰이 없으면 0을 반환한다")
        void deleteExpiredTokens_noExpiredTokens_returnsZero() {
            // given
            Instant valid = Instant.now().plus(1, ChronoUnit.HOURS);
            createAndSaveRefreshToken(testUser, valid);
            createAndSaveRefreshToken(testUser, valid);

            // when
            int deletedCount = refreshTokenCleanupService.deleteExpiredTokens();

            // then
            assertThat(deletedCount).isEqualTo(0);
            assertThat(refreshTokenRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("여러 만료된 토큰이 한 번에 삭제된다")
        void deleteExpiredTokens_multipleExpiredTokens_deletesAll() {
            // given
            Instant expired1 = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant expired2 = Instant.now().minus(2, ChronoUnit.HOURS);
            Instant expired3 = Instant.now().minus(3, ChronoUnit.HOURS);

            createAndSaveRefreshToken(testUser, expired1);
            createAndSaveRefreshToken(testUser, expired2);
            createAndSaveRefreshToken(testUser, expired3);

            // when
            int deletedCount = refreshTokenCleanupService.deleteExpiredTokens();

            // then
            assertThat(deletedCount).isEqualTo(3);
            assertThat(refreshTokenRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("토큰이 없으면 0을 반환한다")
        void deleteExpiredTokens_noTokens_returnsZero() {
            // when
            int deletedCount = refreshTokenCleanupService.deleteExpiredTokens();

            // then
            assertThat(deletedCount).isEqualTo(0);
        }

        @Test
        @DisplayName("경계 시간: 정확히 만료 시간이 지난 토큰은 삭제된다")
        void deleteExpiredTokens_exactlyExpired_isDeleted() {
            // given
            Instant justExpired = Instant.now().minus(1, ChronoUnit.SECONDS);
            RefreshToken token = createAndSaveRefreshToken(testUser, justExpired);

            // when
            int deletedCount = refreshTokenCleanupService.deleteExpiredTokens();

            // then
            assertThat(deletedCount).isEqualTo(1);
            assertThat(refreshTokenRepository.findById(token.getId())).isEmpty();
        }
    }
}
