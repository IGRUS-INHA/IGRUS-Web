package igrus.web.user.semester.service;

import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.semester.domain.SemesterMember;
import igrus.web.user.semester.dto.response.*;
import igrus.web.user.semester.exception.InvalidSemesterException;
import igrus.web.user.semester.repository.SemesterMemberRepository;
import igrus.web.user.semester.repository.SemesterMemberWithUserProjection;
import igrus.web.user.semester.repository.SemesterSummaryProjection;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("SemesterMemberService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class SemesterMemberServiceTest {

    @InjectMocks
    private SemesterMemberService semesterMemberService;

    @Mock
    private SemesterMemberRepository semesterMemberRepository;

    @Mock
    private UserRepository userRepository;

    private User createTestUser(Long id, String studentId, UserRole role) {
        User user = User.create(studentId, "테스트" + id, studentId + "@inha.edu",
                "010-" + studentId, "컴퓨터공학과", "동기", Gender.MALE, 1);
        user.changeRole(role);
        user.verifyEmail();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private SemesterMemberWithUserProjection createMemberProjection(
            Long userId, String studentId, String name, String department,
            String email, String phoneNumber, String role, boolean deleted) {
        SemesterMemberWithUserProjection projection = mock(SemesterMemberWithUserProjection.class);
        given(projection.getUserId()).willReturn(userId);
        given(projection.getStudentId()).willReturn(studentId);
        given(projection.getName()).willReturn(name);
        given(projection.getDepartment()).willReturn(department);
        given(projection.getEmail()).willReturn(email);
        given(projection.getPhoneNumber()).willReturn(phoneNumber);
        given(projection.getMemberRole()).willReturn(role);
        given(projection.getDeleted()).willReturn(deleted);
        return projection;
    }

    private SemesterSummaryProjection createSummaryProjection(int year, int semester, long memberCount) {
        SemesterSummaryProjection projection = mock(SemesterSummaryProjection.class);
        given(projection.getSemesterYear()).willReturn(year);
        given(projection.getSemesterTerm()).willReturn(semester);
        given(projection.getMemberCount()).willReturn(memberCount);
        return projection;
    }

    @Nested
    @DisplayName("getCandidateMembers")
    class GetCandidateMembers {

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
            List<CandidateMemberResponse> result = semesterMemberService.getCandidateMembers(year, semester);

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
            assertThatThrownBy(() -> semesterMemberService.getCandidateMembers(2026, 0))
                    .isInstanceOf(InvalidSemesterException.class);
            assertThatThrownBy(() -> semesterMemberService.getCandidateMembers(2026, 3))
                    .isInstanceOf(InvalidSemesterException.class);
        }

        @DisplayName("유효하지 않은 연도로 조회 시 InvalidSemesterException이 발생한다")
        @Test
        void getCandidateMembers_invalidYear_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> semesterMemberService.getCandidateMembers(1999, 1))
                    .isInstanceOf(InvalidSemesterException.class);
            assertThatThrownBy(() -> semesterMemberService.getCandidateMembers(2101, 1))
                    .isInstanceOf(InvalidSemesterException.class);
        }

        @DisplayName("PENDING_VERIFICATION 상태인 사용자는 후보 목록에 포함되지 않는다")
        @Test
        void getCandidateMembers_pendingUser_excludedFromCandidates() {
            // given
            User pendingUser = User.create("20200010", "보류유저", "20200010@inha.edu",
                    "010-20200010", "컴퓨터공학과", "동기", Gender.MALE, 1);
            ReflectionTestUtils.setField(pendingUser, "id", 10L);

            User activeUser = createTestUser(1L, "20200001", UserRole.MEMBER);

            given(userRepository.findAll()).willReturn(List.of(pendingUser, activeUser));
            given(semesterMemberRepository.existsByUserAndYearAndSemester(activeUser, 2026, 1))
                    .willReturn(false);

            // when
            List<CandidateMemberResponse> result = semesterMemberService.getCandidateMembers(2026, 1);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).userId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("registerMembers")
    class RegisterMembers {

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
                    semesterMemberService.registerMembers(2026, 1, List.of(1L, 2L));

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
                    semesterMemberService.registerMembers(2026, 1, List.of(1L));

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
                    semesterMemberService.registerMembers(2026, 1, List.of(999L));

            // then
            assertThat(result.registeredCount()).isEqualTo(0);
            assertThat(result.skippedCount()).isEqualTo(1);
            then(semesterMemberRepository).should(never()).save(any(SemesterMember.class));
        }

        @DisplayName("유효하지 않은 학기로 등록 시 InvalidSemesterException이 발생한다")
        @Test
        void registerMembers_invalidSemester_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> semesterMemberService.registerMembers(2026, 0, List.of(1L)))
                    .isInstanceOf(InvalidSemesterException.class);
            assertThatThrownBy(() -> semesterMemberService.registerMembers(2026, 3, List.of(1L)))
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
                    semesterMemberService.registerMembers(2026, 1, List.of(1L, 2L, 3L, 999L));

            // then
            assertThat(result.registeredCount()).isEqualTo(2);
            assertThat(result.skippedCount()).isEqualTo(2);
            assertThat(result.totalRequested()).isEqualTo(4);
            then(semesterMemberRepository).should(times(2)).save(any(SemesterMember.class));
        }
    }

    @Nested
    @DisplayName("removeMembers")
    class RemoveMembers {

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
            int removedCount = semesterMemberService.removeMembers(2026, 1, List.of(1L, 2L));

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
            int removedCount = semesterMemberService.removeMembers(2026, 1, List.of(999L));

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
            int removedCount = semesterMemberService.removeMembers(2026, 1, List.of(1L));

            // then
            assertThat(removedCount).isEqualTo(0);
            then(semesterMemberRepository).should(never())
                    .deleteByUserAndYearAndSemester(any(User.class), anyInt(), anyInt());
        }

        @DisplayName("유효하지 않은 학기로 제거 시 InvalidSemesterException이 발생한다")
        @Test
        void removeMembers_invalidSemester_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> semesterMemberService.removeMembers(2026, 0, List.of(1L)))
                    .isInstanceOf(InvalidSemesterException.class);
            assertThatThrownBy(() -> semesterMemberService.removeMembers(2026, 3, List.of(1L)))
                    .isInstanceOf(InvalidSemesterException.class);
        }
    }

    @Nested
    @DisplayName("getSemesterList")
    class GetSemesterList {

        @DisplayName("리포지토리 결과를 올바른 매핑으로 학기 목록을 반환한다")
        @Test
        void getSemesterList_repositoryHasData_returnsSemesterListWithCorrectMapping() {
            // given
            SemesterSummaryProjection p1 = createSummaryProjection(2026, 1, 30L);
            SemesterSummaryProjection p2 = createSummaryProjection(2025, 2, 25L);

            given(semesterMemberRepository.findDistinctSemestersWithCount())
                    .willReturn(List.of(p1, p2));

            // when
            List<SemesterSummaryResponse> result = semesterMemberService.getSemesterList();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).year()).isEqualTo(2026);
            assertThat(result.get(0).semester()).isEqualTo(1);
            assertThat(result.get(0).memberCount()).isEqualTo(30L);
            assertThat(result.get(0).displayName()).isEqualTo("2026년 1학기");
            assertThat(result.get(1).year()).isEqualTo(2025);
            assertThat(result.get(1).semester()).isEqualTo(2);
            assertThat(result.get(1).memberCount()).isEqualTo(25L);
        }

        @DisplayName("리포지토리에 데이터가 없으면 빈 리스트를 반환한다")
        @Test
        void getSemesterList_emptyRepository_returnsEmptyList() {
            // given
            given(semesterMemberRepository.findDistinctSemestersWithCount())
                    .willReturn(Collections.emptyList());

            // when
            List<SemesterSummaryResponse> result = semesterMemberService.getSemesterList();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMemberList")
    class GetMemberList {

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
                    semesterMemberService.getMemberList(2026, 1, null);

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
                    semesterMemberService.getMemberList(2026, 1, "홍길동");

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
                    semesterMemberService.getMemberList(2026, 1, "20200002");

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
            List<SemesterMemberListResponse> resultNull = semesterMemberService.getMemberList(2026, 1, null);
            List<SemesterMemberListResponse> resultEmpty = semesterMemberService.getMemberList(2026, 1, "");
            List<SemesterMemberListResponse> resultBlank = semesterMemberService.getMemberList(2026, 1, "   ");

            // then
            assertThat(resultNull).hasSize(2);
            assertThat(resultEmpty).hasSize(2);
            assertThat(resultBlank).hasSize(2);
        }

        @DisplayName("유효하지 않은 학기로 조회 시 InvalidSemesterException이 발생한다")
        @Test
        void getMemberList_invalidSemester_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> semesterMemberService.getMemberList(2026, 0, null))
                    .isInstanceOf(InvalidSemesterException.class);
            assertThatThrownBy(() -> semesterMemberService.getMemberList(2026, 3, null))
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
                    semesterMemberService.getMemberList(2026, 1, null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isWithdrawn()).isTrue();
        }
    }
}
