package igrus.web.common.domain;

import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SoftDeletableEntity 도메인")
class SoftDeletableEntityTest {

    private User createTestUser() {
        return User.create("20231234", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기");
    }

    @Nested
    @DisplayName("delete 메서드")
    class DeleteTest {

        @Test
        @DisplayName("삭제 호출 시 deleted가 true로 변경되고 삭제 정보가 기록된다")
        void delete_SetsDeletedToTrueAndRecordsInfo() {
            // given
            User user = createTestUser();
            Long deletedBy = 1L;

            // when
            user.delete(deletedBy);

            // then
            assertThat(user.isDeleted()).isTrue();
            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.getDeletedBy()).isEqualTo(deletedBy);
        }

        @Test
        @DisplayName("삭제자 ID 없이 삭제 호출 가능")
        void delete_WithNullDeletedBy_SetsDeletedToTrue() {
            // given
            User user = createTestUser();

            // when
            user.delete(null);

            // then
            assertThat(user.isDeleted()).isTrue();
            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.getDeletedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("restore 메서드")
    class RestoreTest {

        @Test
        @DisplayName("복원 호출 시 deleted가 false로 변경되고 삭제 정보가 초기화된다")
        void restore_SetsDeletedToFalseAndClearsInfo() {
            // given
            User user = createTestUser();
            user.delete(1L);

            // when
            user.restore();

            // then
            assertThat(user.isDeleted()).isFalse();
            assertThat(user.getDeletedAt()).isNull();
            assertThat(user.getDeletedBy()).isNull();
        }

        @Test
        @DisplayName("삭제되지 않은 엔티티에 복원 호출해도 정상 동작")
        void restore_OnNonDeletedEntity_RemainsNotDeleted() {
            // given
            User user = createTestUser();

            // when
            user.restore();

            // then
            assertThat(user.isDeleted()).isFalse();
            assertThat(user.getDeletedAt()).isNull();
            assertThat(user.getDeletedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("isDeleted 메서드")
    class IsDeletedTest {

        @Test
        @DisplayName("생성 직후에는 false 반환")
        void isDeleted_WhenNewlyCreated_ReturnsFalse() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(user.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("삭제 후에는 true 반환")
        void isDeleted_AfterDelete_ReturnsTrue() {
            // given
            User user = createTestUser();
            user.delete(1L);

            // when & then
            assertThat(user.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("복원 후에는 false 반환")
        void isDeleted_AfterRestore_ReturnsFalse() {
            // given
            User user = createTestUser();
            user.delete(1L);
            user.restore();

            // when & then
            assertThat(user.isDeleted()).isFalse();
        }
    }
}
