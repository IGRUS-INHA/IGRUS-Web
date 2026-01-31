package igrus.web.user.domain;

import igrus.web.user.domain.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserSuspension 도메인")
class UserSuspensionTest {

    private User createTestUser() {
        return User.create("20231234", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기", Gender.MALE, 1);
    }

    @Nested
    @DisplayName("create 메서드")
    class CreateTest {

        @Test
        @DisplayName("유효한 정지 정보로 UserSuspension 생성 성공")
        void create_WithValidInfo_ReturnsUserSuspension() {
            // given
            User user = createTestUser();
            String reason = "규칙 위반";
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            Long suspendedBy = 1L;

            // when
            UserSuspension suspension = UserSuspension.create(user, reason, suspendedUntil, suspendedBy);

            // then
            assertThat(suspension).isNotNull();
            assertThat(suspension.getUser()).isEqualTo(user);
            assertThat(suspension.getReason()).isEqualTo(reason);
            assertThat(suspension.getSuspendedUntil()).isEqualTo(suspendedUntil);
            assertThat(suspension.getSuspendedBy()).isEqualTo(suspendedBy);
            assertThat(suspension.getSuspendedAt()).isNotNull();
            assertThat(suspension.getLiftedAt()).isNull();
            assertThat(suspension.getLiftedBy()).isNull();
        }

        @Test
        @DisplayName("정지 시작일 지정하여 생성 성공")
        void create_WithSuspendedAt_ReturnsUserSuspension() {
            // given
            User user = createTestUser();
            String reason = "규칙 위반";
            Instant suspendedAt = Instant.now().minus(1, ChronoUnit.DAYS);
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            Long suspendedBy = 1L;

            // when
            UserSuspension suspension = UserSuspension.create(user, reason, suspendedAt, suspendedUntil, suspendedBy);

            // then
            assertThat(suspension).isNotNull();
            assertThat(suspension.getSuspendedAt()).isEqualTo(suspendedAt);
            assertThat(suspension.getSuspendedUntil()).isEqualTo(suspendedUntil);
        }

        @Test
        @DisplayName("정지 종료일이 시작일 이전이면 예외 발생")
        void create_WithInvalidPeriod_ThrowsException() {
            // given
            User user = createTestUser();
            Instant suspendedAt = Instant.now();
            Instant suspendedUntil = suspendedAt.minus(1, ChronoUnit.DAYS);

            // when & then
            assertThatThrownBy(() -> UserSuspension.create(user, "사유", suspendedAt, suspendedUntil, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("정지 종료일은 정지 시작일 이후여야 합니다");
        }
    }

    @Nested
    @DisplayName("lift 메서드")
    class LiftTest {

        @Test
        @DisplayName("정지 해제 성공")
        void lift_WhenNotLifted_SetsLiftedInfo() {
            // given
            User user = createTestUser();
            UserSuspension suspension = UserSuspension.create(user, "사유",
                    Instant.now().plus(7, ChronoUnit.DAYS), 1L);
            Long liftedBy = 2L;

            // when
            suspension.lift(liftedBy);

            // then
            assertThat(suspension.isLifted()).isTrue();
            assertThat(suspension.getLiftedAt()).isNotNull();
            assertThat(suspension.getLiftedBy()).isEqualTo(liftedBy);
        }

        @Test
        @DisplayName("해제일 지정하여 정지 해제 성공")
        void lift_WithLiftedAt_SetsLiftedInfo() {
            // given
            User user = createTestUser();
            UserSuspension suspension = UserSuspension.create(user, "사유",
                    Instant.now().plus(7, ChronoUnit.DAYS), 1L);
            Instant liftedAt = Instant.now();
            Long liftedBy = 2L;

            // when
            suspension.lift(liftedAt, liftedBy);

            // then
            assertThat(suspension.isLifted()).isTrue();
            assertThat(suspension.getLiftedAt()).isEqualTo(liftedAt);
            assertThat(suspension.getLiftedBy()).isEqualTo(liftedBy);
        }

        @Test
        @DisplayName("이미 해제된 정지를 다시 해제하면 예외 발생")
        void lift_WhenAlreadyLifted_ThrowsException() {
            // given
            User user = createTestUser();
            UserSuspension suspension = UserSuspension.create(user, "사유",
                    Instant.now().plus(7, ChronoUnit.DAYS), 1L);
            suspension.lift(2L);

            // when & then
            assertThatThrownBy(() -> suspension.lift(3L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 해제된 정지입니다");
        }
    }

    @Nested
    @DisplayName("isActive 메서드")
    class IsActiveTest {

        @Test
        @DisplayName("현재 유효한 정지는 true 반환")
        void isActive_WhenCurrentlyActive_ReturnsTrue() {
            // given
            User user = createTestUser();
            Instant suspendedAt = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            UserSuspension suspension = UserSuspension.create(user, "사유", suspendedAt, suspendedUntil, 1L);

            // when & then
            assertThat(suspension.isActive()).isTrue();
        }

        @Test
        @DisplayName("해제된 정지는 false 반환")
        void isActive_WhenLifted_ReturnsFalse() {
            // given
            User user = createTestUser();
            Instant suspendedAt = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            UserSuspension suspension = UserSuspension.create(user, "사유", suspendedAt, suspendedUntil, 1L);
            suspension.lift(2L);

            // when & then
            assertThat(suspension.isActive()).isFalse();
        }

        @Test
        @DisplayName("만료된 정지는 false 반환")
        void isActive_WhenExpired_ReturnsFalse() {
            // given
            User user = createTestUser();
            Instant suspendedAt = Instant.now().minus(7, ChronoUnit.DAYS);
            Instant suspendedUntil = Instant.now().minus(1, ChronoUnit.DAYS);
            UserSuspension suspension = UserSuspension.create(user, "사유", suspendedAt, suspendedUntil, 1L);

            // when & then
            assertThat(suspension.isActive()).isFalse();
        }

        @Test
        @DisplayName("아직 시작되지 않은 정지는 false 반환")
        void isActive_WhenNotStarted_ReturnsFalse() {
            // given
            User user = createTestUser();
            Instant suspendedAt = Instant.now().plus(1, ChronoUnit.DAYS);
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            UserSuspension suspension = UserSuspension.create(user, "사유", suspendedAt, suspendedUntil, 1L);

            // when & then
            assertThat(suspension.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpired 메서드")
    class IsExpiredTest {

        @Test
        @DisplayName("만료된 정지는 true 반환")
        void isExpired_WhenExpired_ReturnsTrue() {
            // given
            User user = createTestUser();
            Instant suspendedAt = Instant.now().minus(7, ChronoUnit.DAYS);
            Instant suspendedUntil = Instant.now().minus(1, ChronoUnit.DAYS);
            UserSuspension suspension = UserSuspension.create(user, "사유", suspendedAt, suspendedUntil, 1L);

            // when & then
            assertThat(suspension.isExpired()).isTrue();
        }

        @Test
        @DisplayName("만료되지 않은 정지는 false 반환")
        void isExpired_WhenNotExpired_ReturnsFalse() {
            // given
            User user = createTestUser();
            Instant suspendedAt = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            UserSuspension suspension = UserSuspension.create(user, "사유", suspendedAt, suspendedUntil, 1L);

            // when & then
            assertThat(suspension.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasStarted 메서드")
    class HasStartedTest {

        @Test
        @DisplayName("시작된 정지는 true 반환")
        void hasStarted_WhenStarted_ReturnsTrue() {
            // given
            User user = createTestUser();
            Instant suspendedAt = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            UserSuspension suspension = UserSuspension.create(user, "사유", suspendedAt, suspendedUntil, 1L);

            // when & then
            assertThat(suspension.hasStarted()).isTrue();
        }

        @Test
        @DisplayName("아직 시작되지 않은 정지는 false 반환")
        void hasStarted_WhenNotStarted_ReturnsFalse() {
            // given
            User user = createTestUser();
            Instant suspendedAt = Instant.now().plus(1, ChronoUnit.DAYS);
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            UserSuspension suspension = UserSuspension.create(user, "사유", suspendedAt, suspendedUntil, 1L);

            // when & then
            assertThat(suspension.hasStarted()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateReason 메서드")
    class UpdateReasonTest {

        @Test
        @DisplayName("사유 업데이트 성공")
        void updateReason_WithValidReason_UpdatesReason() {
            // given
            User user = createTestUser();
            UserSuspension suspension = UserSuspension.create(user, "초기 사유",
                    Instant.now().plus(7, ChronoUnit.DAYS), 1L);

            // when
            suspension.updateReason("업데이트된 사유");

            // then
            assertThat(suspension.getReason()).isEqualTo("업데이트된 사유");
        }

        @Test
        @DisplayName("null로 사유 업데이트 시 예외 발생")
        void updateReason_WithNull_ThrowsException() {
            // given
            User user = createTestUser();
            UserSuspension suspension = UserSuspension.create(user, "초기 사유",
                    Instant.now().plus(7, ChronoUnit.DAYS), 1L);

            // when & then
            assertThatThrownBy(() -> suspension.updateReason(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("정지 사유는 필수입니다");
        }

        @Test
        @DisplayName("빈 문자열로 사유 업데이트 시 예외 발생")
        void updateReason_WithBlank_ThrowsException() {
            // given
            User user = createTestUser();
            UserSuspension suspension = UserSuspension.create(user, "초기 사유",
                    Instant.now().plus(7, ChronoUnit.DAYS), 1L);

            // when & then
            assertThatThrownBy(() -> suspension.updateReason("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("정지 사유는 필수입니다");
        }
    }

    @Nested
    @DisplayName("extendSuspension 메서드")
    class ExtendSuspensionTest {

        @Test
        @DisplayName("정지 기간 연장 성공")
        void extendSuspension_WithValidDate_ExtendsSuspension() {
            // given
            User user = createTestUser();
            Instant originalUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            UserSuspension suspension = UserSuspension.create(user, "사유", originalUntil, 1L);
            Instant newUntil = originalUntil.plus(7, ChronoUnit.DAYS);

            // when
            suspension.extendSuspension(newUntil);

            // then
            assertThat(suspension.getSuspendedUntil()).isEqualTo(newUntil);
        }

        @Test
        @DisplayName("해제된 정지 연장 시 예외 발생")
        void extendSuspension_WhenLifted_ThrowsException() {
            // given
            User user = createTestUser();
            UserSuspension suspension = UserSuspension.create(user, "사유",
                    Instant.now().plus(7, ChronoUnit.DAYS), 1L);
            suspension.lift(2L);

            // when & then
            assertThatThrownBy(() -> suspension.extendSuspension(Instant.now().plus(14, ChronoUnit.DAYS)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("해제된 정지는 연장할 수 없습니다");
        }

        @Test
        @DisplayName("기존 종료일 이전으로 연장 시 예외 발생")
        void extendSuspension_WithEarlierDate_ThrowsException() {
            // given
            User user = createTestUser();
            Instant originalUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            UserSuspension suspension = UserSuspension.create(user, "사유", originalUntil, 1L);
            Instant earlierDate = originalUntil.minus(1, ChronoUnit.DAYS);

            // when & then
            assertThatThrownBy(() -> suspension.extendSuspension(earlierDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("새로운 종료일은 기존 종료일 이후여야 합니다");
        }
    }

    @Nested
    @DisplayName("isLifted 메서드")
    class IsLiftedTest {

        @Test
        @DisplayName("해제되지 않은 정지는 false 반환")
        void isLifted_WhenNotLifted_ReturnsFalse() {
            // given
            User user = createTestUser();
            UserSuspension suspension = UserSuspension.create(user, "사유",
                    Instant.now().plus(7, ChronoUnit.DAYS), 1L);

            // when & then
            assertThat(suspension.isLifted()).isFalse();
        }

        @Test
        @DisplayName("해제된 정지는 true 반환")
        void isLifted_WhenLifted_ReturnsTrue() {
            // given
            User user = createTestUser();
            UserSuspension suspension = UserSuspension.create(user, "사유",
                    Instant.now().plus(7, ChronoUnit.DAYS), 1L);
            suspension.lift(2L);

            // when & then
            assertThat(suspension.isLifted()).isTrue();
        }
    }
}
