package igrus.web.security.auth.password.domain;

import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordResetToken 도메인")
class PasswordResetTokenTest {

    private User createTestUser() {
        return User.create("20231234", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기");
    }

    @Nested
    @DisplayName("create 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("유효한 정보로 PasswordResetToken 생성 성공 [PWD-003]")
        void create_WithValidInfo_ReturnsPasswordResetToken() {
            // given
            User user = createTestUser();
            String token = "test-token-uuid";
            long expiryMillis = 1800000L; // 30분

            // when
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, expiryMillis);

            // then
            assertThat(resetToken).isNotNull();
            assertThat(resetToken.getUser()).isEqualTo(user);
            assertThat(resetToken.getToken()).isEqualTo(token);
            assertThat(resetToken.isUsed()).isFalse();
        }

        @Test
        @DisplayName("생성 시 만료 시간이 현재 시간 + expiryMillis로 설정 [PWD-003]")
        void create_ExpiresAt_IsSetCorrectly() {
            // given
            User user = createTestUser();
            String token = "test-token-uuid";
            long expiryMillis = 1800000L; // 30분
            Instant before = Instant.now();

            // when
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, expiryMillis);

            // then
            Instant after = Instant.now();
            Instant expectedMin = before.plusMillis(expiryMillis);
            Instant expectedMax = after.plusMillis(expiryMillis);

            assertThat(resetToken.getExpiresAt())
                    .isAfterOrEqualTo(expectedMin)
                    .isBeforeOrEqualTo(expectedMax);
        }

        @Test
        @DisplayName("생성 시 used 상태는 false [PWD-003]")
        void create_UsedStatus_IsFalse() {
            // given
            User user = createTestUser();
            String token = "test-token-uuid";
            long expiryMillis = 1800000L;

            // when
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, expiryMillis);

            // then
            assertThat(resetToken.isUsed()).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpired 메서드")
    class IsExpiredTest {

        @Test
        @DisplayName("만료 시간이 지나지 않은 토큰은 false 반환 [PWD-010]")
        void isExpired_BeforeExpiry_ReturnsFalse() {
            // given
            User user = createTestUser();
            String token = "test-token-uuid";
            long expiryMillis = 1800000L; // 30분

            PasswordResetToken resetToken = PasswordResetToken.create(user, token, expiryMillis);

            // when & then
            assertThat(resetToken.isExpired()).isFalse();
        }

        @Test
        @DisplayName("만료 시간이 지난 토큰은 true 반환 [PWD-020]")
        void isExpired_AfterExpiry_ReturnsTrue() throws Exception {
            // given
            User user = createTestUser();
            String token = "test-token-uuid";
            long expiryMillis = 1L; // 1ms (즉시 만료)

            PasswordResetToken resetToken = PasswordResetToken.create(user, token, expiryMillis);

            // 만료 시간을 과거로 설정
            Field expiresAtField = PasswordResetToken.class.getDeclaredField("expiresAt");
            expiresAtField.setAccessible(true);
            expiresAtField.set(resetToken, Instant.now().minusSeconds(60));

            // when & then
            assertThat(resetToken.isExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("isValid 메서드")
    class IsValidTest {

        @Test
        @DisplayName("만료되지 않고 사용되지 않은 토큰은 true 반환 [PWD-010]")
        void isValid_NotExpiredAndNotUsed_ReturnsTrue() {
            // given
            User user = createTestUser();
            String token = "test-token-uuid";
            long expiryMillis = 1800000L;

            PasswordResetToken resetToken = PasswordResetToken.create(user, token, expiryMillis);

            // when & then
            assertThat(resetToken.isValid()).isTrue();
        }

        @Test
        @DisplayName("만료된 토큰은 false 반환 [PWD-020]")
        void isValid_WhenExpired_ReturnsFalse() throws Exception {
            // given
            User user = createTestUser();
            String token = "test-token-uuid";
            long expiryMillis = 1L;

            PasswordResetToken resetToken = PasswordResetToken.create(user, token, expiryMillis);

            // 만료 시간을 과거로 설정
            Field expiresAtField = PasswordResetToken.class.getDeclaredField("expiresAt");
            expiresAtField.setAccessible(true);
            expiresAtField.set(resetToken, Instant.now().minusSeconds(60));

            // when & then
            assertThat(resetToken.isValid()).isFalse();
        }

        @Test
        @DisplayName("이미 사용된 토큰은 false 반환 [PWD-021]")
        void isValid_WhenUsed_ReturnsFalse() {
            // given
            User user = createTestUser();
            String token = "test-token-uuid";
            long expiryMillis = 1800000L;

            PasswordResetToken resetToken = PasswordResetToken.create(user, token, expiryMillis);
            resetToken.markAsUsed();

            // when & then
            assertThat(resetToken.isValid()).isFalse();
        }

        @Test
        @DisplayName("만료되고 사용된 토큰은 false 반환")
        void isValid_WhenExpiredAndUsed_ReturnsFalse() throws Exception {
            // given
            User user = createTestUser();
            String token = "test-token-uuid";
            long expiryMillis = 1L;

            PasswordResetToken resetToken = PasswordResetToken.create(user, token, expiryMillis);
            resetToken.markAsUsed();

            // 만료 시간을 과거로 설정
            Field expiresAtField = PasswordResetToken.class.getDeclaredField("expiresAt");
            expiresAtField.setAccessible(true);
            expiresAtField.set(resetToken, Instant.now().minusSeconds(60));

            // when & then
            assertThat(resetToken.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("markAsUsed 메서드")
    class MarkAsUsedTest {

        @Test
        @DisplayName("markAsUsed 호출 시 used 상태가 true로 변경 [PWD-021]")
        void markAsUsed_SetsUsedToTrue() {
            // given
            User user = createTestUser();
            String token = "test-token-uuid";
            long expiryMillis = 1800000L;

            PasswordResetToken resetToken = PasswordResetToken.create(user, token, expiryMillis);

            // when
            resetToken.markAsUsed();

            // then
            assertThat(resetToken.isUsed()).isTrue();
        }

        @Test
        @DisplayName("이미 사용된 토큰에 다시 markAsUsed 호출해도 true 유지")
        void markAsUsed_WhenAlreadyUsed_StaysTrue() {
            // given
            User user = createTestUser();
            String token = "test-token-uuid";
            long expiryMillis = 1800000L;

            PasswordResetToken resetToken = PasswordResetToken.create(user, token, expiryMillis);
            resetToken.markAsUsed();

            // when
            resetToken.markAsUsed();

            // then
            assertThat(resetToken.isUsed()).isTrue();
        }
    }
}
