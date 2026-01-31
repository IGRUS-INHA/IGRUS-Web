package igrus.web.user.semester.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.semester.dto.response.CandidateMemberResponse;
import igrus.web.user.semester.dto.response.RegisterSemesterMembersResponse;
import igrus.web.user.semester.dto.response.SemesterMemberListResponse;
import igrus.web.user.semester.dto.response.SemesterSummaryResponse;
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

@DisplayName("SemesterMemberService 통합 테스트")
class SemesterMemberServiceIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private SemesterMemberService semesterMemberService;

    @Autowired
    private SemesterMemberRepository semesterMemberRepository;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private User createAndSaveUserWithName(String studentId, String email, UserRole role, String name) {
        User user = createAndSaveUser(studentId, email, role);
        setField(user, "name", name);
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("US1: 회원 등록")
    class Registration {

        @Test
        @DisplayName("학기에 회원을 등록하면 올바른 역할 스냅샷으로 SemesterMember 레코드가 생성된다")
        void registerMembers_withValidUsers_createsSemesterMembersWithCorrectRoleSnapshot() {
            // given
            User member = createAndSaveUser("11111111", "member@inha.edu", UserRole.MEMBER);
            User operator = createAndSaveUser("22222222", "operator@inha.edu", UserRole.OPERATOR);

            // when
            RegisterSemesterMembersResponse response = semesterMemberService.registerMembers(
                    2026, 1, List.of(member.getId(), operator.getId()));

            // then
            assertThat(response.registeredCount()).isEqualTo(2);
            assertThat(response.skippedCount()).isEqualTo(0);
            assertThat(response.totalRequested()).isEqualTo(2);

            assertThat(semesterMemberRepository.countByYearAndSemester(2026, 1)).isEqualTo(2);

            List<SemesterMemberListResponse> members = semesterMemberService.getMemberList(2026, 1, null);
            assertThat(members).hasSize(2);
            assertThat(members).extracting(SemesterMemberListResponse::role)
                    .containsExactlyInAnyOrder(UserRole.MEMBER, UserRole.OPERATOR);
        }

        @Test
        @DisplayName("이미 등록된 회원을 다시 등록하면 건너뛴다 (멱등성)")
        void registerMembers_withAlreadyRegisteredUser_skipsDuplicate() {
            // given
            User user = createAndSaveUser("11111111", "user@inha.edu", UserRole.MEMBER);
            semesterMemberService.registerMembers(2026, 1, List.of(user.getId()));

            // when
            RegisterSemesterMembersResponse response = semesterMemberService.registerMembers(
                    2026, 1, List.of(user.getId()));

            // then
            assertThat(response.registeredCount()).isEqualTo(0);
            assertThat(response.skippedCount()).isEqualTo(1);
            assertThat(response.totalRequested()).isEqualTo(1);

            assertThat(semesterMemberRepository.countByYearAndSemester(2026, 1)).isEqualTo(1);
        }

        @Test
        @DisplayName("등록 후보 회원 목록 조회 시 등록 상태가 올바르게 표시된다")
        void getCandidateMembers_afterPartialRegistration_showsCorrectStatus() {
            // given
            User registered = createAndSaveUser("11111111", "reg@inha.edu", UserRole.MEMBER);
            User notRegistered = createAndSaveUser("22222222", "noreg@inha.edu", UserRole.MEMBER);
            semesterMemberService.registerMembers(2026, 1, List.of(registered.getId()));

            // when
            List<CandidateMemberResponse> candidates = semesterMemberService.getCandidateMembers(2026, 1);

            // then
            assertThat(candidates).hasSize(2);

            CandidateMemberResponse registeredCandidate = candidates.stream()
                    .filter(c -> c.userId().equals(registered.getId()))
                    .findFirst().orElseThrow();
            assertThat(registeredCandidate.alreadyRegistered()).isTrue();

            CandidateMemberResponse notRegisteredCandidate = candidates.stream()
                    .filter(c -> c.userId().equals(notRegistered.getId()))
                    .findFirst().orElseThrow();
            assertThat(notRegisteredCandidate.alreadyRegistered()).isFalse();
        }
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
            semesterMemberService.registerMembers(2026, 1, List.of(user1.getId(), user2.getId()));

            // when
            int removedCount = semesterMemberService.removeMembers(2026, 1, List.of(user1.getId()));

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
            int removedCount = semesterMemberService.removeMembers(2026, 1, List.of(user.getId()));

            // then
            assertThat(removedCount).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("US3: 명단 조회")
    class Query {

        @Test
        @DisplayName("학기 목록 조회 시 연도/학기/회원 수가 올바르게 반환된다")
        void getSemesterList_withMultipleSemesters_returnsCorrectSummary() {
            // given
            User user1 = createAndSaveUser("11111111", "user1@inha.edu", UserRole.MEMBER);
            User user2 = createAndSaveUser("22222222", "user2@inha.edu", UserRole.MEMBER);
            User user3 = createAndSaveUser("33333333", "user3@inha.edu", UserRole.OPERATOR);

            semesterMemberService.registerMembers(2025, 2, List.of(user1.getId(), user2.getId()));
            semesterMemberService.registerMembers(2026, 1, List.of(user1.getId(), user2.getId(), user3.getId()));

            // when
            List<SemesterSummaryResponse> semesters = semesterMemberService.getSemesterList();

            // then
            assertThat(semesters).hasSize(2);

            SemesterSummaryResponse first = semesters.get(0);
            assertThat(first.year()).isEqualTo(2026);
            assertThat(first.semester()).isEqualTo(1);
            assertThat(first.memberCount()).isEqualTo(3);
            assertThat(first.displayName()).isEqualTo("2026년 1학기");

            SemesterSummaryResponse second = semesters.get(1);
            assertThat(second.year()).isEqualTo(2025);
            assertThat(second.semester()).isEqualTo(2);
            assertThat(second.memberCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("회원 명단 조회 시 탈퇴한 사용자도 포함된다")
        void getMemberList_withWithdrawnUser_includesWithdrawnUser() {
            // given
            User activeUser = createAndSaveUser("11111111", "active@inha.edu", UserRole.MEMBER);
            User withdrawnUser = createAndSaveUser("22222222", "withdrawn@inha.edu", UserRole.MEMBER);

            semesterMemberService.registerMembers(2026, 1,
                    List.of(activeUser.getId(), withdrawnUser.getId()));

            // 등록 이후에 탈퇴 처리 (native query로 직접 업데이트)
            transactionTemplate.execute(status -> {
                entityManager.createNativeQuery(
                        "UPDATE users SET users_status = 'WITHDRAWN', users_deleted = true, users_deleted_at = NOW() " +
                                "WHERE users_id = :userId")
                        .setParameter("userId", withdrawnUser.getId())
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
                return null;
            });

            // when
            List<SemesterMemberListResponse> members = semesterMemberService.getMemberList(2026, 1, null);

            // then
            assertThat(members).hasSize(2);

            SemesterMemberListResponse withdrawn = members.stream()
                    .filter(m -> m.userId().equals(withdrawnUser.getId()))
                    .findFirst().orElseThrow();
            assertThat(withdrawn.isWithdrawn()).isTrue();

            SemesterMemberListResponse active = members.stream()
                    .filter(m -> m.userId().equals(activeUser.getId()))
                    .findFirst().orElseThrow();
            assertThat(active.isWithdrawn()).isFalse();
        }

        @Test
        @DisplayName("회원 명단 조회 시 이름 키워드로 필터링된다")
        void getMemberList_withNameKeyword_filtersCorrectly() {
            // given
            User kim = createAndSaveUserWithName("11111111", "kim@inha.edu", UserRole.MEMBER, "김철수");
            User lee = createAndSaveUserWithName("22222222", "lee@inha.edu", UserRole.MEMBER, "이영희");
            User park = createAndSaveUserWithName("33333333", "park@inha.edu", UserRole.MEMBER, "박철수");

            semesterMemberService.registerMembers(2026, 1,
                    List.of(kim.getId(), lee.getId(), park.getId()));

            // when
            List<SemesterMemberListResponse> results = semesterMemberService.getMemberList(2026, 1, "철수");

            // then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(SemesterMemberListResponse::name)
                    .containsExactlyInAnyOrder("김철수", "박철수");
        }

        @Test
        @DisplayName("회원 명단 조회 시 학번 키워드로 필터링된다")
        void getMemberList_withStudentIdKeyword_filtersCorrectly() {
            // given
            User user1 = createAndSaveUser("12345678", "user1@inha.edu", UserRole.MEMBER);
            User user2 = createAndSaveUser("22222222", "user2@inha.edu", UserRole.MEMBER);
            User user3 = createAndSaveUser("12340000", "user3@inha.edu", UserRole.MEMBER);

            semesterMemberService.registerMembers(2026, 1,
                    List.of(user1.getId(), user2.getId(), user3.getId()));

            // when
            List<SemesterMemberListResponse> results = semesterMemberService.getMemberList(2026, 1, "1234");

            // then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(SemesterMemberListResponse::studentId)
                    .containsExactlyInAnyOrder("12345678", "12340000");
        }
    }

    @Nested
    @DisplayName("Validation: 유효하지 않은 학기 검증")
    class Validation {

        @Test
        @DisplayName("학기가 0이면 InvalidSemesterException이 발생한다")
        void registerMembers_withSemesterZero_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> semesterMemberService.registerMembers(2026, 0, List.of()))
                    .isInstanceOf(InvalidSemesterException.class);
        }

        @Test
        @DisplayName("학기가 3이면 InvalidSemesterException이 발생한다")
        void registerMembers_withSemesterThree_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> semesterMemberService.registerMembers(2026, 3, List.of()))
                    .isInstanceOf(InvalidSemesterException.class);
        }

        @Test
        @DisplayName("연도가 1999이면 InvalidSemesterException이 발생한다")
        void registerMembers_withYear1999_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> semesterMemberService.registerMembers(1999, 1, List.of()))
                    .isInstanceOf(InvalidSemesterException.class);
        }

        @Test
        @DisplayName("연도가 2101이면 InvalidSemesterException이 발생한다")
        void registerMembers_withYear2101_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> semesterMemberService.registerMembers(2101, 1, List.of()))
                    .isInstanceOf(InvalidSemesterException.class);
        }

        @Test
        @DisplayName("getCandidateMembers에서도 유효하지 않은 학기 시 InvalidSemesterException이 발생한다")
        void getCandidateMembers_withInvalidSemester_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> semesterMemberService.getCandidateMembers(2026, 0))
                    .isInstanceOf(InvalidSemesterException.class);
        }

        @Test
        @DisplayName("getMemberList에서도 유효하지 않은 연도 시 InvalidSemesterException이 발생한다")
        void getMemberList_withInvalidYear_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> semesterMemberService.getMemberList(1999, 1, null))
                    .isInstanceOf(InvalidSemesterException.class);
        }

        @Test
        @DisplayName("removeMembers에서도 유효하지 않은 학기 시 InvalidSemesterException이 발생한다")
        void removeMembers_withInvalidSemester_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> semesterMemberService.removeMembers(2026, 3, List.of()))
                    .isInstanceOf(InvalidSemesterException.class);
        }
    }
}
