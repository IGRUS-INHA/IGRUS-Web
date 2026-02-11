package igrus.web.user.semester.service.write;

import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.Interest;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.semester.domain.SemesterMember;
import igrus.web.user.semester.dto.response.RegisterSemesterMembersResponse;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@DisplayName("RegisterSemesterMembersService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RegisterSemesterMembersServiceTest {

    @InjectMocks
    private RegisterSemesterMembersService registerSemesterMembersService;

    @Mock
    private SemesterMemberRepository semesterMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private SemesterValidator semesterValidator;

    private User createTestUser(Long id, String studentId, UserRole role) {
        User user = User.create(studentId, "테스트" + id, studentId + "@inha.edu",
                "010-" + studentId.substring(0, 4) + "-" + studentId.substring(4), "컴퓨터공학과", "동기", List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED, List.of(), null, null, null);
        user.changeRole(role);
        user.verifyEmail();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @DisplayName("신규 회원을 성공적으로 등록하고 올바른 카운트를 반환한다")
    @Test
    void registerMembers_newMembers_registersSuccessfullyAndReturnsCorrectCounts() {
        // given
        User user1 = createTestUser(1L, "20200001", UserRole.MEMBER);
        User user2 = createTestUser(2L, "20200002", UserRole.MEMBER);

        given(userRepository.findById(1L)).willReturn(Optional.of(user1));
        given(userRepository.findById(2L)).willReturn(Optional.of(user2));
        given(semesterMemberRepository.existsByUserAndYearAndSemester(user1, 2026, 1)).willReturn(false);
        given(semesterMemberRepository.existsByUserAndYearAndSemester(user2, 2026, 1)).willReturn(false);

        // when
        RegisterSemesterMembersResponse result =
                registerSemesterMembersService.registerMembers(2026, 1, List.of(1L, 2L));

        // then
        assertThat(result.registeredCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isEqualTo(0);
        assertThat(result.totalRequested()).isEqualTo(2);
        then(semesterMemberRepository).should(times(2)).save(any(SemesterMember.class));
    }

    @DisplayName("이미 등록된 회원은 건너뛰고 skippedCount가 증가한다")
    @Test
    void registerMembers_alreadyRegisteredMembers_skippedCountIncrements() {
        // given
        User user1 = createTestUser(1L, "20200001", UserRole.MEMBER);

        given(userRepository.findById(1L)).willReturn(Optional.of(user1));
        given(semesterMemberRepository.existsByUserAndYearAndSemester(user1, 2026, 1)).willReturn(true);

        // when
        RegisterSemesterMembersResponse result =
                registerSemesterMembersService.registerMembers(2026, 1, List.of(1L));

        // then
        assertThat(result.registeredCount()).isEqualTo(0);
        assertThat(result.skippedCount()).isEqualTo(1);
        then(semesterMemberRepository).should(never()).save(any(SemesterMember.class));
    }

    @DisplayName("존재하지 않는 사용자 ID는 건너뛴다")
    @Test
    void registerMembers_nonExistentUserIds_skippedSilently() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        RegisterSemesterMembersResponse result =
                registerSemesterMembersService.registerMembers(2026, 1, List.of(999L));

        // then
        assertThat(result.registeredCount()).isEqualTo(0);
        assertThat(result.skippedCount()).isEqualTo(1);
        then(semesterMemberRepository).should(never()).save(any(SemesterMember.class));
    }

    @DisplayName("유효하지 않은 학기로 등록 시 InvalidSemesterException이 발생한다")
    @Test
    void registerMembers_invalidSemester_throwsInvalidSemesterException() {
        assertThatThrownBy(() -> registerSemesterMembersService.registerMembers(2026, 0, List.of(1L)))
                .isInstanceOf(InvalidSemesterException.class);
        assertThatThrownBy(() -> registerSemesterMembersService.registerMembers(2026, 3, List.of(1L)))
                .isInstanceOf(InvalidSemesterException.class);
    }

    @DisplayName("신규 회원과 기존 회원이 혼합되면 올바른 registered/skipped 카운트를 반환한다")
    @Test
    void registerMembers_mixOfNewAndExisting_returnsCorrectCounts() {
        // given
        User user1 = createTestUser(1L, "20200001", UserRole.MEMBER);
        User user2 = createTestUser(2L, "20200002", UserRole.MEMBER);
        User user3 = createTestUser(3L, "20200003", UserRole.OPERATOR);

        given(userRepository.findById(1L)).willReturn(Optional.of(user1));
        given(userRepository.findById(2L)).willReturn(Optional.of(user2));
        given(userRepository.findById(3L)).willReturn(Optional.of(user3));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        given(semesterMemberRepository.existsByUserAndYearAndSemester(user1, 2026, 1)).willReturn(false);
        given(semesterMemberRepository.existsByUserAndYearAndSemester(user2, 2026, 1)).willReturn(true);
        given(semesterMemberRepository.existsByUserAndYearAndSemester(user3, 2026, 1)).willReturn(false);

        // when
        RegisterSemesterMembersResponse result =
                registerSemesterMembersService.registerMembers(2026, 1, List.of(1L, 2L, 3L, 999L));

        // then
        assertThat(result.registeredCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isEqualTo(2);
        assertThat(result.totalRequested()).isEqualTo(4);
        then(semesterMemberRepository).should(times(2)).save(any(SemesterMember.class));
    }
}
