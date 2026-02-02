package igrus.web.user.semester.service.write;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.semester.exception.InvalidSemesterException;
import igrus.web.user.semester.repository.SemesterMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RemoveSemesterMembersService 통합 테스트")
class RemoveSemesterMembersServiceIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private RemoveSemesterMembersService removeSemesterMembersService;

    @Autowired
    private RegisterSemesterMembersService registerSemesterMembersService;

    @Autowired
    private SemesterMemberRepository semesterMemberRepository;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("US2: 회원 제외")
    class Removal {

        @Test
        @DisplayName("학기에서 회원을 제외하면 SemesterMember 레코드가 삭제된다")
        void removeMembers_withRegisteredUsers_deletesSemesterMembers() {
            // given
            User user1 = createAndSaveUser("11111111", "user1@inha.edu", UserRole.MEMBER);
            User user2 = createAndSaveUser("22222222", "user2@inha.edu", UserRole.MEMBER);
            registerSemesterMembersService.registerMembers(2026, 1, List.of(user1.getId(), user2.getId()));

            // when
            int removedCount = removeSemesterMembersService.removeMembers(2026, 1, List.of(user1.getId()));

            // then
            assertThat(removedCount).isEqualTo(1);
            assertThat(semesterMemberRepository.countByYearAndSemester(2026, 1)).isEqualTo(1);
        }

        @Test
        @DisplayName("등록되지 않은 회원을 제외하면 조용히 건너뛴다")
        void removeMembers_withNonRegisteredUser_silentlySkips() {
            // given
            User user = createAndSaveUser("11111111", "user@inha.edu", UserRole.MEMBER);

            // when
            int removedCount = removeSemesterMembersService.removeMembers(2026, 1, List.of(user.getId()));

            // then
            assertThat(removedCount).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("removeMembers에서도 유효하지 않은 학기 시 InvalidSemesterException이 발생한다")
        void removeMembers_withInvalidSemester_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> removeSemesterMembersService.removeMembers(2026, 3, List.of()))
                    .isInstanceOf(InvalidSemesterException.class);
        }
    }
}
