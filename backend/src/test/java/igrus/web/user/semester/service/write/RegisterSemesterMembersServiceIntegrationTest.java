package igrus.web.user.semester.service.write;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.semester.dto.response.RegisterSemesterMembersResponse;
import igrus.web.user.semester.dto.response.SemesterMemberListResponse;
import igrus.web.user.semester.exception.InvalidSemesterException;
import igrus.web.user.semester.repository.SemesterMemberRepository;
import igrus.web.user.semester.service.read.GetSemesterMemberListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RegisterSemesterMembersService 통합 테스트")
class RegisterSemesterMembersServiceIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private RegisterSemesterMembersService registerSemesterMembersService;

    @Autowired
    private GetSemesterMemberListService getSemesterMemberListService;

    @Autowired
    private SemesterMemberRepository semesterMemberRepository;

    @BeforeEach
    void setUp() {
        setUpBase();
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
            RegisterSemesterMembersResponse response = registerSemesterMembersService.registerMembers(
                    2026, 1, List.of(member.getId(), operator.getId()));

            // then
            assertThat(response.registeredCount()).isEqualTo(2);
            assertThat(response.skippedCount()).isEqualTo(0);
            assertThat(response.totalRequested()).isEqualTo(2);

            assertThat(semesterMemberRepository.countByYearAndSemester(2026, 1)).isEqualTo(2);

            List<SemesterMemberListResponse> members = getSemesterMemberListService.getMemberList(2026, 1, null);
            assertThat(members).hasSize(2);
            assertThat(members).extracting(SemesterMemberListResponse::role)
                    .containsExactlyInAnyOrder(UserRole.MEMBER, UserRole.OPERATOR);
        }

        @Test
        @DisplayName("이미 등록된 회원을 다시 등록하면 건너뛴다 (멱등성)")
        void registerMembers_withAlreadyRegisteredUser_skipsDuplicate() {
            // given
            User user = createAndSaveUser("11111111", "user@inha.edu", UserRole.MEMBER);
            registerSemesterMembersService.registerMembers(2026, 1, List.of(user.getId()));

            // when
            RegisterSemesterMembersResponse response = registerSemesterMembersService.registerMembers(
                    2026, 1, List.of(user.getId()));

            // then
            assertThat(response.registeredCount()).isEqualTo(0);
            assertThat(response.skippedCount()).isEqualTo(1);
            assertThat(response.totalRequested()).isEqualTo(1);

            assertThat(semesterMemberRepository.countByYearAndSemester(2026, 1)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Validation: 유효하지 않은 학기 검증")
    class Validation {

        @Test
        @DisplayName("학기가 0이면 InvalidSemesterException이 발생한다")
        void registerMembers_withSemesterZero_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> registerSemesterMembersService.registerMembers(2026, 0, List.of()))
                    .isInstanceOf(InvalidSemesterException.class);
        }

        @Test
        @DisplayName("학기가 3이면 InvalidSemesterException이 발생한다")
        void registerMembers_withSemesterThree_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> registerSemesterMembersService.registerMembers(2026, 3, List.of()))
                    .isInstanceOf(InvalidSemesterException.class);
        }

        @Test
        @DisplayName("연도가 1999이면 InvalidSemesterException이 발생한다")
        void registerMembers_withYear1999_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> registerSemesterMembersService.registerMembers(1999, 1, List.of()))
                    .isInstanceOf(InvalidSemesterException.class);
        }

        @Test
        @DisplayName("연도가 2101이면 InvalidSemesterException이 발생한다")
        void registerMembers_withYear2101_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> registerSemesterMembersService.registerMembers(2101, 1, List.of()))
                    .isInstanceOf(InvalidSemesterException.class);
        }
    }
}
