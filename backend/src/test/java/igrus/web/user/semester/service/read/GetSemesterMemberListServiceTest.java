package igrus.web.user.semester.service.read;

import igrus.web.user.domain.UserRole;
import igrus.web.user.semester.dto.response.SemesterMemberListResponse;
import igrus.web.user.semester.exception.InvalidSemesterException;
import igrus.web.user.semester.repository.SemesterMemberRepository;
import igrus.web.user.semester.repository.SemesterMemberWithUserProjection;
import igrus.web.user.semester.service.support.SemesterValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("GetSemesterMemberListService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class GetSemesterMemberListServiceTest {

    @InjectMocks
    private GetSemesterMemberListService getSemesterMemberListService;

    @Mock
    private SemesterMemberRepository semesterMemberRepository;

    @Spy
    private SemesterValidator semesterValidator;

    private SemesterMemberWithUserProjection createMemberProjection(
            Long userId, String studentId, String name, String department,
            String email, String phoneNumber, String role, boolean deleted) {
        return createMemberProjection(userId, studentId, name, department,
                email, phoneNumber, role, deleted, 1, "동기");
    }

    private SemesterMemberWithUserProjection createMemberProjection(
            Long userId, String studentId, String name, String department,
            String email, String phoneNumber, String role, boolean deleted,
            Integer grade, String motivation) {
        SemesterMemberWithUserProjection projection = mock(SemesterMemberWithUserProjection.class);
        given(projection.getUserId()).willReturn(userId);
        given(projection.getStudentId()).willReturn(studentId);
        given(projection.getName()).willReturn(name);
        given(projection.getDepartment()).willReturn(department);
        given(projection.getEmail()).willReturn(email);
        given(projection.getPhoneNumber()).willReturn(phoneNumber);
        given(projection.getMemberRole()).willReturn(role);
        given(projection.getDeleted()).willReturn(deleted);
        given(projection.getGrade()).willReturn(grade);
        given(projection.getMotivation()).willReturn(motivation);
        return projection;
    }

    @DisplayName("Projection 결과를 올바르게 매핑하여 회원 목록을 반환한다")
    @Test
    void getMemberList_projectionResults_returnsMappedMemberList() {
        // given
        SemesterMemberWithUserProjection p1 = createMemberProjection(
                1L, "20200001", "홍길동", "컴퓨터공학과", "20200001@inha.edu", "010-1234-5678", "MEMBER", false);
        SemesterMemberWithUserProjection p2 = createMemberProjection(
                2L, "20200002", "김철수", "전자공학과", "20200002@inha.edu", "010-9876-5432", "OPERATOR", false);

        given(semesterMemberRepository.findAllWithUserIncludingDeleted(2026, 1))
                .willReturn(List.of(p1, p2));

        // when
        List<SemesterMemberListResponse> result =
                getSemesterMemberListService.getMemberList(2026, 1, null);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).userId()).isEqualTo(1L);
        assertThat(result.get(0).studentId()).isEqualTo("20200001");
        assertThat(result.get(0).name()).isEqualTo("홍길동");
        assertThat(result.get(0).department()).isEqualTo("컴퓨터공학과");
        assertThat(result.get(0).email()).isEqualTo("20200001@inha.edu");
        assertThat(result.get(0).phoneNumber()).isEqualTo("010-1234-5678");
        assertThat(result.get(0).role()).isEqualTo(UserRole.MEMBER);
        assertThat(result.get(0).isWithdrawn()).isFalse();
        assertThat(result.get(1).userId()).isEqualTo(2L);
        assertThat(result.get(1).role()).isEqualTo(UserRole.OPERATOR);
    }

    @DisplayName("키워드로 이름 필터링이 올바르게 동작한다")
    @Test
    void getMemberList_keywordMatchesName_returnsFilteredResults() {
        // given
        SemesterMemberWithUserProjection p1 = createMemberProjection(
                1L, "20200001", "홍길동", "컴퓨터공학과", "20200001@inha.edu", "010-1234-5678", "MEMBER", false);
        SemesterMemberWithUserProjection p2 = createMemberProjection(
                2L, "20200002", "김철수", "전자공학과", "20200002@inha.edu", "010-9876-5432", "MEMBER", false);

        given(semesterMemberRepository.findAllWithUserIncludingDeleted(2026, 1))
                .willReturn(List.of(p1, p2));

        // when
        List<SemesterMemberListResponse> result =
                getSemesterMemberListService.getMemberList(2026, 1, "홍길동");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("홍길동");
    }

    @DisplayName("키워드로 학번 필터링이 올바르게 동작한다")
    @Test
    void getMemberList_keywordMatchesStudentId_returnsFilteredResults() {
        // given
        SemesterMemberWithUserProjection p1 = createMemberProjection(
                1L, "20200001", "홍길동", "컴퓨터공학과", "20200001@inha.edu", "010-1234-5678", "MEMBER", false);
        SemesterMemberWithUserProjection p2 = createMemberProjection(
                2L, "20200002", "김철수", "전자공학과", "20200002@inha.edu", "010-9876-5432", "MEMBER", false);

        given(semesterMemberRepository.findAllWithUserIncludingDeleted(2026, 1))
                .willReturn(List.of(p1, p2));

        // when
        List<SemesterMemberListResponse> result =
                getSemesterMemberListService.getMemberList(2026, 1, "20200002");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentId()).isEqualTo("20200002");
    }

    @DisplayName("빈 문자열 또는 null 키워드는 전체 결과를 반환한다")
    @Test
    void getMemberList_emptyOrNullKeyword_returnsAll() {
        // given
        SemesterMemberWithUserProjection p1 = createMemberProjection(
                1L, "20200001", "홍길동", "컴퓨터공학과", "20200001@inha.edu", "010-1234-5678", "MEMBER", false);
        SemesterMemberWithUserProjection p2 = createMemberProjection(
                2L, "20200002", "김철수", "전자공학과", "20200002@inha.edu", "010-9876-5432", "MEMBER", false);

        given(semesterMemberRepository.findAllWithUserIncludingDeleted(2026, 1))
                .willReturn(List.of(p1, p2));

        // when
        List<SemesterMemberListResponse> resultNull = getSemesterMemberListService.getMemberList(2026, 1, null);
        List<SemesterMemberListResponse> resultEmpty = getSemesterMemberListService.getMemberList(2026, 1, "");
        List<SemesterMemberListResponse> resultBlank = getSemesterMemberListService.getMemberList(2026, 1, "   ");

        // then
        assertThat(resultNull).hasSize(2);
        assertThat(resultEmpty).hasSize(2);
        assertThat(resultBlank).hasSize(2);
    }

    @DisplayName("유효하지 않은 학기로 조회 시 InvalidSemesterException이 발생한다")
    @Test
    void getMemberList_invalidSemester_throwsInvalidSemesterException() {
        assertThatThrownBy(() -> getSemesterMemberListService.getMemberList(2026, 0, null))
                .isInstanceOf(InvalidSemesterException.class);
        assertThatThrownBy(() -> getSemesterMemberListService.getMemberList(2026, 3, null))
                .isInstanceOf(InvalidSemesterException.class);
    }

    @DisplayName("탈퇴한 사용자는 isWithdrawn이 true로 반환된다")
    @Test
    void getMemberList_deletedUser_isWithdrawnTrue() {
        // given
        SemesterMemberWithUserProjection p = createMemberProjection(
                1L, "20200001", "홍길동", "컴퓨터공학과", "20200001@inha.edu", "010-1234-5678", "MEMBER", true);

        given(semesterMemberRepository.findAllWithUserIncludingDeleted(2026, 1))
                .willReturn(List.of(p));

        // when
        List<SemesterMemberListResponse> result =
                getSemesterMemberListService.getMemberList(2026, 1, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isWithdrawn()).isTrue();
    }
}
