package igrus.web.user.semester.service.read;

import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.semester.dto.response.CandidateMemberResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@DisplayName("GetCandidateMembersService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class GetCandidateMembersServiceTest {

    @InjectMocks
    private GetCandidateMembersService getCandidateMembersService;

    @Mock
    private SemesterMemberRepository semesterMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private SemesterValidator semesterValidator;

    private User createTestUser(Long id, String studentId, UserRole role) {
        User user = User.create(studentId, "테스트" + id, studentId + "@inha.edu",
                "010-" + studentId.substring(0, 4) + "-" + studentId.substring(4), "컴퓨터공학과", "동기", List.of(), Gender.MALE, 1);
        user.changeRole(role);
        user.verifyEmail();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @DisplayName("유효한 연도/학기로 조회 시 등록 상태를 포함한 후보 목록을 반환한다")
    @Test
    void getCandidateMembers_validYearAndSemester_returnsCandidateListWithRegistrationStatus() {
        // given
        int year = 2026;
        int semester = 1;

        User activeAssociate = createTestUser(1L, "20200001", UserRole.ASSOCIATE);
        User activeMember = createTestUser(2L, "20200002", UserRole.MEMBER);
        User activeOperator = createTestUser(3L, "20200003", UserRole.OPERATOR);

        given(userRepository.findAll()).willReturn(List.of(activeAssociate, activeMember, activeOperator));
        given(semesterMemberRepository.existsByUserAndYearAndSemester(activeAssociate, year, semester))
                .willReturn(false);
        given(semesterMemberRepository.existsByUserAndYearAndSemester(activeMember, year, semester))
                .willReturn(true);
        given(semesterMemberRepository.existsByUserAndYearAndSemester(activeOperator, year, semester))
                .willReturn(false);

        // when
        List<CandidateMemberResponse> result = getCandidateMembersService.getCandidateMembers(year, semester);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).userId()).isEqualTo(1L);
        assertThat(result.get(0).alreadyRegistered()).isFalse();
        assertThat(result.get(1).userId()).isEqualTo(2L);
        assertThat(result.get(1).alreadyRegistered()).isTrue();
        assertThat(result.get(2).userId()).isEqualTo(3L);
        assertThat(result.get(2).alreadyRegistered()).isFalse();
    }

    @DisplayName("유효하지 않은 학기(0 또는 3)로 조회 시 InvalidSemesterException이 발생한다")
    @Test
    void getCandidateMembers_invalidSemester_throwsInvalidSemesterException() {
        assertThatThrownBy(() -> getCandidateMembersService.getCandidateMembers(2026, 0))
                .isInstanceOf(InvalidSemesterException.class);
        assertThatThrownBy(() -> getCandidateMembersService.getCandidateMembers(2026, 3))
                .isInstanceOf(InvalidSemesterException.class);
    }

    @DisplayName("유효하지 않은 연도로 조회 시 InvalidSemesterException이 발생한다")
    @Test
    void getCandidateMembers_invalidYear_throwsInvalidSemesterException() {
        assertThatThrownBy(() -> getCandidateMembersService.getCandidateMembers(1999, 1))
                .isInstanceOf(InvalidSemesterException.class);
        assertThatThrownBy(() -> getCandidateMembersService.getCandidateMembers(2101, 1))
                .isInstanceOf(InvalidSemesterException.class);
    }

    @DisplayName("PENDING_VERIFICATION 상태인 사용자는 후보 목록에 포함되지 않는다")
    @Test
    void getCandidateMembers_pendingUser_excludedFromCandidates() {
        // given
        User pendingUser = User.create("20200010", "보류유저", "20200010@inha.edu",
                "010-2020-0010", "컴퓨터공학과", "동기", List.of(), Gender.MALE, 1);
        ReflectionTestUtils.setField(pendingUser, "id", 10L);

        User activeUser = createTestUser(1L, "20200001", UserRole.MEMBER);

        given(userRepository.findAll()).willReturn(List.of(pendingUser, activeUser));
        given(semesterMemberRepository.existsByUserAndYearAndSemester(activeUser, 2026, 1))
                .willReturn(false);

        // when
        List<CandidateMemberResponse> result = getCandidateMembersService.getCandidateMembers(2026, 1);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(1L);
    }
}
