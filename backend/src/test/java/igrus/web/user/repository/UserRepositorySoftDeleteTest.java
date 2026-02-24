package igrus.web.user.repository;

import igrus.web.common.config.JpaAuditingConfig;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@DisplayName("UserRepository Soft Delete 통합 테스트")
class UserRepositorySoftDeleteTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User createAndSaveUser(String studentId, String email, String phoneNumber) {
        User user = User.create(studentId, "홍길동", email, phoneNumber, "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED, List.of(), null, null, null);
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();
        return user;
    }

    private void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.delete(1L);
        userRepository.saveAndFlush(user);
        entityManager.clear();
    }

    @Nested
    @DisplayName("기본 조회 (soft delete 필터링)")
    class BasicQueryTest {

        @Test
        @DisplayName("삭제되지 않은 사용자는 findById로 조회 가능")
        void findById_WhenNotDeleted_ReturnsUser() {
            // given
            User savedUser = createAndSaveUser("20231001", "test1@inha.edu", "010-1111-1111");

            // when
            Optional<User> foundUser = userRepository.findById(savedUser.getId());

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getStudentId()).isEqualTo("20231001");
        }

        @Test
        @DisplayName("soft delete된 사용자는 findById로 조회되지 않음")
        void findById_WhenDeleted_ReturnsEmpty() {
            // given
            User savedUser = createAndSaveUser("20231002", "test2@inha.edu", "010-2222-2222");
            softDeleteUser(savedUser.getId());

            // when
            Optional<User> foundUser = userRepository.findById(savedUser.getId());

            // then
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("soft delete된 사용자는 findByEmail로 조회되지 않음")
        void findByEmail_WhenDeleted_ReturnsEmpty() {
            // given
            User savedUser = createAndSaveUser("20231003", "test3@inha.edu", "010-3333-3333");
            softDeleteUser(savedUser.getId());

            // when
            Optional<User> foundUser = userRepository.findByEmail("test3@inha.edu");

            // then
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("soft delete된 사용자는 existsByEmail로 false 반환")
        void existsByEmail_WhenDeleted_ReturnsFalse() {
            // given
            User savedUser = createAndSaveUser("20231004", "test4@inha.edu", "010-4444-4444");
            softDeleteUser(savedUser.getId());

            // when
            boolean exists = userRepository.existsByEmail("test4@inha.edu");

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("삭제된 데이터 포함 조회")
    class IncludingDeletedQueryTest {

        @Test
        @DisplayName("findByIdIncludingDeleted로 soft delete된 사용자도 조회 가능")
        void findByIdIncludingDeleted_WhenDeleted_ReturnsUser() {
            // given
            User savedUser = createAndSaveUser("20231005", "test5@inha.edu", "010-5555-5555");
            softDeleteUser(savedUser.getId());

            // when
            Optional<User> foundUser = userRepository.findByIdIncludingDeleted(savedUser.getId());

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().isDeleted()).isTrue();
            assertThat(foundUser.get().getDeletedBy()).isEqualTo(1L);
        }

        @Test
        @DisplayName("findByEmailIncludingDeleted로 soft delete된 사용자도 조회 가능")
        void findByEmailIncludingDeleted_WhenDeleted_ReturnsUser() {
            // given
            User savedUser = createAndSaveUser("20231006", "test6@inha.edu", "010-6666-6666");
            softDeleteUser(savedUser.getId());

            // when
            Optional<User> foundUser = userRepository.findByEmailIncludingDeleted("test6@inha.edu");

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("복원 테스트")
    class RestoreTest {

        @Test
        @DisplayName("복원된 사용자는 기본 조회로 다시 조회 가능")
        void restore_ThenFindById_ReturnsUser() {
            // given
            User savedUser = createAndSaveUser("20231007", "test7@inha.edu", "010-7777-7777");
            softDeleteUser(savedUser.getId());

            // when
            User deletedUser = userRepository.findByIdIncludingDeleted(savedUser.getId()).orElseThrow();
            deletedUser.restore();
            userRepository.saveAndFlush(deletedUser);
            entityManager.clear();

            // then
            Optional<User> restoredUser = userRepository.findById(savedUser.getId());
            assertThat(restoredUser).isPresent();
            assertThat(restoredUser.get().isDeleted()).isFalse();
        }
    }
}
