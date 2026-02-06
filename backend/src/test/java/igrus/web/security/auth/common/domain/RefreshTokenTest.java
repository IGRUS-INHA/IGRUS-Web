package igrus.web.security.auth.common.domain;

import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshToken 도메인")
class RefreshTokenTest {

    private static final String TEST_TOKEN_FAMILY = "test-family-uuid";

    private User createTestUser() {
        return User.create("20231234", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기", Gender.MALE, 1);
    }

    @Nested
    @DisplayName("create 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("유효한 정보로 RefreshToken 생성 성공")
        void create_WithValidInfo_ReturnsRefreshToken() {
            // given
            User user = createTestUser();
            String token = "test-refresh-token";
            long expiryMillis = 259200000L; // 3일

            // when
            RefreshToken refreshToken = RefreshToken.create(user, token, expiryMillis, TEST_TOKEN_FAMILY);

            // then
            assertThat(refreshToken).isNotNull();
            assertThat(refreshToken.getUser()).isEqualTo(user);
            assertThat(refreshToken.getToken()).isEqualTo(token);
            assertThat(refreshToken.getTokenFamily()).isEqualTo(TEST_TOKEN_FAMILY);
        }

        @Test
        @DisplayName("생성 시 expiresAt이 현재 시간 + expiryMillis로 설정")
        void create_ExpiresAt_SetToCurrentTimePlusExpiry() {
            // given
            User user = createTestUser();
            String token = "test-refresh-token";
            long expiryMillis = 259200000L; // 3일

            Instant beforeCreate = Instant.now();

            // when
            RefreshToken refreshToken = RefreshToken.create(user, token, expiryMillis, TEST_TOKEN_FAMILY);

            // then
            Instant afterCreate = Instant.now();
            Instant expectedMinExpiry = beforeCreate.plusMillis(expiryMillis);
            Instant expectedMaxExpiry = afterCreate.plusMillis(expiryMillis);

            assertThat(refreshToken.getExpiresAt())
                    .isAfterOrEqualTo(expectedMinExpiry)
                    .isBeforeOrEqualTo(expectedMaxExpiry);
        }

        @Test
        @DisplayName("생성 시 revoked는 기본값 false")
        void create_Revoked_DefaultFalse() {
            // given
            User user = createTestUser();
            String token = "test-refresh-token";
            long expiryMillis = 259200000L;

            // when
            RefreshToken refreshToken = RefreshToken.create(user, token, expiryMillis, TEST_TOKEN_FAMILY);

            // then
            assertThat(refreshToken.isRevoked()).isFalse();
        }
    }

    @Nested
    @DisplayName("createInitial 정적 팩토리 메서드")
    class CreateInitialTest {

        @Test
        @DisplayName("createInitial로 생성 시 tokenFamily가 자동 생성")
        void createInitial_GeneratesTokenFamily() {
            // given
            User user = createTestUser();

            // when
            RefreshToken refreshToken = RefreshToken.createInitial(user, "token", 259200000L);

            // then
            assertThat(refreshToken.getTokenFamily()).isNotNull();
            assertThat(refreshToken.getTokenFamily()).isNotEmpty();
        }

        @Test
        @DisplayName("createInitial로 두 번 생성 시 서로 다른 tokenFamily")
        void createInitial_TwoCalls_DifferentFamilies() {
            // given
            User user = createTestUser();

            // when
            RefreshToken token1 = RefreshToken.createInitial(user, "token1", 259200000L);
            RefreshToken token2 = RefreshToken.createInitial(user, "token2", 259200000L);

            // then
            assertThat(token1.getTokenFamily()).isNotEqualTo(token2.getTokenFamily());
        }
    }

    @Nested
    @DisplayName("isExpired 메서드")
    class IsExpiredTest {

        @Test
        @DisplayName("만료 시간이 지난 경우 true 반환")
        void isExpired_WhenExpired_ReturnsTrue() throws Exception {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 1000L, TEST_TOKEN_FAMILY);

            // 리플렉션으로 expiresAt을 과거 시간으로 설정
            Field expiresAtField = RefreshToken.class.getDeclaredField("expiresAt");
            expiresAtField.setAccessible(true);
            expiresAtField.set(refreshToken, Instant.now().minusMillis(1000L));

            // when
            boolean expired = refreshToken.isExpired();

            // then
            assertThat(expired).isTrue();
        }

        @Test
        @DisplayName("만료 시간이 지나지 않은 경우 false 반환")
        void isExpired_WhenNotExpired_ReturnsFalse() {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 259200000L, TEST_TOKEN_FAMILY);

            // when
            boolean expired = refreshToken.isExpired();

            // then
            assertThat(expired).isFalse();
        }
    }

    @Nested
    @DisplayName("isValid 메서드")
    class IsValidTest {

        @Test
        @DisplayName("만료되지 않고 폐기되지 않은 경우 true 반환")
        void isValid_WhenNotExpiredAndNotRevoked_ReturnsTrue() {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 259200000L, TEST_TOKEN_FAMILY);

            // when
            boolean valid = refreshToken.isValid();

            // then
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("만료된 경우 false 반환")
        void isValid_WhenExpired_ReturnsFalse() throws Exception {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 1000L, TEST_TOKEN_FAMILY);

            // 리플렉션으로 expiresAt을 과거 시간으로 설정
            Field expiresAtField = RefreshToken.class.getDeclaredField("expiresAt");
            expiresAtField.setAccessible(true);
            expiresAtField.set(refreshToken, Instant.now().minusMillis(1000L));

            // when
            boolean valid = refreshToken.isValid();

            // then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("폐기된 경우 false 반환")
        void isValid_WhenRevoked_ReturnsFalse() {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 259200000L, TEST_TOKEN_FAMILY);
            refreshToken.revoke();

            // when
            boolean valid = refreshToken.isValid();

            // then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("만료되고 폐기된 경우 false 반환")
        void isValid_WhenExpiredAndRevoked_ReturnsFalse() throws Exception {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 1000L, TEST_TOKEN_FAMILY);
            refreshToken.revoke();

            // 리플렉션으로 expiresAt을 과거 시간으로 설정
            Field expiresAtField = RefreshToken.class.getDeclaredField("expiresAt");
            expiresAtField.setAccessible(true);
            expiresAtField.set(refreshToken, Instant.now().minusMillis(1000L));

            // when
            boolean valid = refreshToken.isValid();

            // then
            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("revoke 메서드")
    class RevokeTest {

        @Test
        @DisplayName("revoke 호출 시 revoked가 true로 변경되고 revokedAt이 설정")
        void revoke_ChangesRevokedToTrueAndSetsRevokedAt() {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 259200000L, TEST_TOKEN_FAMILY);
            assertThat(refreshToken.isRevoked()).isFalse();
            assertThat(refreshToken.getRevokedAt()).isNull();

            Instant beforeRevoke = Instant.now();

            // when
            refreshToken.revoke();

            // then
            assertThat(refreshToken.isRevoked()).isTrue();
            assertThat(refreshToken.getRevokedAt()).isNotNull();
            assertThat(refreshToken.getRevokedAt()).isAfterOrEqualTo(beforeRevoke);
        }

        @Test
        @DisplayName("이미 폐기된 토큰에 revoke 호출해도 정상 동작")
        void revoke_WhenAlreadyRevoked_StaysRevoked() {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 259200000L, TEST_TOKEN_FAMILY);
            refreshToken.revoke();

            // when
            refreshToken.revoke();

            // then
            assertThat(refreshToken.isRevoked()).isTrue();
        }
    }

    @Nested
    @DisplayName("rotateWith 메서드")
    class RotateWithTest {

        @Test
        @DisplayName("rotateWith 호출 시 토큰이 폐기되고 교체 토큰이 기록")
        void rotateWith_RevokesAndRecordsReplacement() {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "old-token", 259200000L, TEST_TOKEN_FAMILY);
            String newToken = "new-token";

            Instant beforeRotate = Instant.now();

            // when
            refreshToken.rotateWith(newToken);

            // then
            assertThat(refreshToken.isRevoked()).isTrue();
            assertThat(refreshToken.getRevokedAt()).isAfterOrEqualTo(beforeRotate);
            assertThat(refreshToken.getReplacedByToken()).isEqualTo(newToken);
        }
    }

    @Nested
    @DisplayName("isWithinGracePeriod 메서드")
    class IsWithinGracePeriodTest {

        @Test
        @DisplayName("폐기되지 않은 토큰은 Grace Period 내가 아님")
        void isWithinGracePeriod_WhenNotRevoked_ReturnsFalse() {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 259200000L, TEST_TOKEN_FAMILY);

            // when
            boolean withinGracePeriod = refreshToken.isWithinGracePeriod(Duration.ofSeconds(10));

            // then
            assertThat(withinGracePeriod).isFalse();
        }

        @Test
        @DisplayName("폐기 직후 토큰은 Grace Period 내")
        void isWithinGracePeriod_WhenJustRevoked_ReturnsTrue() {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 259200000L, TEST_TOKEN_FAMILY);
            refreshToken.revoke();

            // when
            boolean withinGracePeriod = refreshToken.isWithinGracePeriod(Duration.ofSeconds(10));

            // then
            assertThat(withinGracePeriod).isTrue();
        }

        @Test
        @DisplayName("Grace Period가 지난 토큰은 false 반환")
        void isWithinGracePeriod_WhenPastGracePeriod_ReturnsFalse() throws Exception {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 259200000L, TEST_TOKEN_FAMILY);
            refreshToken.revoke();

            // revokedAt을 과거로 설정 (11초 전)
            Field revokedAtField = RefreshToken.class.getDeclaredField("revokedAt");
            revokedAtField.setAccessible(true);
            revokedAtField.set(refreshToken, Instant.now().minusSeconds(11));

            // when
            boolean withinGracePeriod = refreshToken.isWithinGracePeriod(Duration.ofSeconds(10));

            // then
            assertThat(withinGracePeriod).isFalse();
        }
    }
}
