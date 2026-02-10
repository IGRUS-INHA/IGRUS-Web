package igrus.web.user.semester.service.write;

import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.semester.exception.InvalidSemesterException;
import igrus.web.user.semester.repository.SemesterMemberRepository;
import igrus.web.user.semester.service.support.SemesterValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@DisplayName("RemoveSemesterMembersService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RemoveSemesterMembersServiceTest {

    @InjectMocks
    private RemoveSemesterMembersService removeSemesterMembersService;

    @Mock
    private SemesterMemberRepository semesterMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private SemesterValidator semesterValidator;

    private User createTestUser(Long id, String studentId, UserRole role) {
        User user = User.create(studentId, "테스트" + id, studentId + "@inha.edu",
                "010-" + studentId.substring(0, 4) + "-" + studentId.substring(4), "컴퓨터공학과", "동기", Gender.MALE, 1);
        user.changeRole(role);
        user.verifyEmail();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @DisplayName("등록된 회원을 제거하고 올바른 카운트를 반환한다")
    @Test
    void removeMembers_existingMembers_removesAndReturnsCorrectCount() {
        // given
        User user1 = createTestUser(1L, "20200001", UserRole.MEMBER);
        User user2 = createTestUser(2L, "20200002", UserRole.MEMBER);

        given(userRepository.findById(1L)).willReturn(Optional.of(user1));
        given(userRepository.findById(2L)).willReturn(Optional.of(user2));
        given(semesterMemberRepository.existsByUserAndYearAndSemester(user1, 2026, 1)).willReturn(true);
        given(semesterMemberRepository.existsByUserAndYearAndSemester(user2, 2026, 1)).willReturn(true);

        // when
        int removedCount = removeSemesterMembersService.removeMembers(2026, 1, List.of(1L, 2L));

        // then
        assertThat(removedCount).isEqualTo(2);
        then(semesterMemberRepository).should(times(2))
                .deleteByUserAndYearAndSemester(any(User.class), eq(2026), eq(1));
    }

    @DisplayName("존재하지 않는 사용자 ID는 조용히 건너뛴다")
    @Test
    void removeMembers_nonExistentUserIds_skippedSilently() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        int removedCount = removeSemesterMembersService.removeMembers(2026, 1, List.of(999L));

        // then
        assertThat(removedCount).isEqualTo(0);
        then(semesterMemberRepository).should(never())
                .deleteByUserAndYearAndSemester(any(User.class), anyInt(), anyInt());
    }

    @DisplayName("해당 학기에 미등록된 회원은 조용히 건너뛴다")
    @Test
    void removeMembers_nonRegisteredMembers_skippedSilently() {
        // given
        User user1 = createTestUser(1L, "20200001", UserRole.MEMBER);

        given(userRepository.findById(1L)).willReturn(Optional.of(user1));
        given(semesterMemberRepository.existsByUserAndYearAndSemester(user1, 2026, 1)).willReturn(false);

        // when
        int removedCount = removeSemesterMembersService.removeMembers(2026, 1, List.of(1L));

        // then
        assertThat(removedCount).isEqualTo(0);
        then(semesterMemberRepository).should(never())
                .deleteByUserAndYearAndSemester(any(User.class), anyInt(), anyInt());
    }

    @DisplayName("유효하지 않은 학기로 제거 시 InvalidSemesterException이 발생한다")
    @Test
    void removeMembers_invalidSemester_throwsInvalidSemesterException() {
        assertThatThrownBy(() -> removeSemesterMembersService.removeMembers(2026, 0, List.of(1L)))
                .isInstanceOf(InvalidSemesterException.class);
        assertThatThrownBy(() -> removeSemesterMembersService.removeMembers(2026, 3, List.of(1L)))
                .isInstanceOf(InvalidSemesterException.class);
    }
}
