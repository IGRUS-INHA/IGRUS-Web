package igrus.web.security.auth.common.domain;

import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshToken 도메인")
class RefreshTokenTest {

    private User createTestUser() {
        return User.create("20231234", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기");
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
            long expiryMillis = 604800000L; // 7일

            // when
            RefreshToken refreshToken = RefreshToken.create(user, token, expiryMillis);

            // then
            assertThat(refreshToken).isNotNull();
            assertThat(refreshToken.getUser()).isEqualTo(user);
            assertThat(refreshToken.getToken()).isEqualTo(token);
        }

        @Test
        @DisplayName("생성 시 expiresAt이 현재 시간 + expiryMillis로 설정")
        void create_ExpiresAt_SetToCurrentTimePlusExpiry() {
            // given
            User user = createTestUser();
            String token = "test-refresh-token";
            long expiryMillis = 604800000L; // 7일

            Instant beforeCreate = Instant.now();

            // when
            RefreshToken refreshToken = RefreshToken.create(user, token, expiryMillis);

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
            long expiryMillis = 604800000L;

            // when
            RefreshToken refreshToken = RefreshToken.create(user, token, expiryMillis);

            // then
            assertThat(refreshToken.isRevoked()).isFalse();
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
            RefreshToken refreshToken = RefreshToken.create(user, "token", 1000L);

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
            RefreshToken refreshToken = RefreshToken.create(user, "token", 604800000L); // 7일

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
            RefreshToken refreshToken = RefreshToken.create(user, "token", 604800000L);

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
            RefreshToken refreshToken = RefreshToken.create(user, "token", 1000L);

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
            RefreshToken refreshToken = RefreshToken.create(user, "token", 604800000L);
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
            RefreshToken refreshToken = RefreshToken.create(user, "token", 1000L);
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
        @DisplayName("revoke 호출 시 revoked가 true로 변경")
        void revoke_ChangesRevokedToTrue() {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 604800000L);
            assertThat(refreshToken.isRevoked()).isFalse();

            // when
            refreshToken.revoke();

            // then
            assertThat(refreshToken.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("이미 폐기된 토큰에 revoke 호출해도 정상 동작")
        void revoke_WhenAlreadyRevoked_StaysRevoked() {
            // given
            User user = createTestUser();
            RefreshToken refreshToken = RefreshToken.create(user, "token", 604800000L);
            refreshToken.revoke();

            // when
            refreshToken.revoke();

            // then
            assertThat(refreshToken.isRevoked()).isTrue();
        }
    }
}
